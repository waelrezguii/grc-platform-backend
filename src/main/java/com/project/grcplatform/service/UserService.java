package com.project.grcplatform.service;

import com.project.grcplatform.constant.AuditAction;
import com.project.grcplatform.constant.AuditEntityType;
import com.project.grcplatform.constant.ErrorCode;
import com.project.grcplatform.config.LoginSecurityProperties;
import com.project.grcplatform.dto.LoginRequestDTO;
import com.project.grcplatform.dto.LoginResult;
import com.project.grcplatform.dto.RegisterRequestDTO;
import com.project.grcplatform.dto.UpdatePasswordDTO;
import com.project.grcplatform.dto.UpdateUserDTO;
import com.project.grcplatform.dto.UserResponseDTO;
import com.project.grcplatform.exception.AppException;
import com.project.grcplatform.mapper.UserMapper;
import com.project.grcplatform.model.Organisation;
import com.project.grcplatform.model.Permission;
import com.project.grcplatform.model.Role;
import com.project.grcplatform.model.User;
import com.project.grcplatform.model.UserGrade;
import com.project.grcplatform.model.PasswordResetToken;
import com.project.grcplatform.repository.OrganisationRepository;
import com.project.grcplatform.repository.PasswordResetTokenRepository;
import com.project.grcplatform.repository.UserSessionRepository;
import com.project.grcplatform.repository.PermissionRepository;
import com.project.grcplatform.repository.RoleRepository;
import com.project.grcplatform.repository.UserGradeRepository;
import com.project.grcplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrganisationRepository organisationRepository;
    private final UserGradeRepository userGradeRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AuditService auditService;
    private final EmailService emailService;
    private final OtpService otpService;
    private final LoginSecurityProperties loginSecurityProperties;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserSessionRepository userSessionRepository;

    private static final String ALLOWED_DOMAIN = "@company.com";
    private static final String TEMP_PASSWORD_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";

    private String generateTempPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(TEMP_PASSWORD_CHARS.charAt(random.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    // -------------------- LOGIN --------------------
    @Transactional
    public LoginResult login(LoginRequestDTO dto) {
        Optional<User> userOpt = userRepository.findByEmail(dto.getEmail());

        // Unknown email — fail without leaking user existence
        if (userOpt.isEmpty()) {
            auditService.logFailure(AuditAction.USER_LOGIN_FAILED, AuditEntityType.USER,
                    null, "Failed login for unknown email: " + dto.getEmail(), "User not found");
            throw new AppException(ErrorCode.INVALID_EMAIL_OR_PASSWORD);
        }

        User user = userOpt.get();

        // Account disabled
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AppException(ErrorCode.ACCOUNT_DISABLED);
        }

        // Lockout check
        if (user.getLockedUntil() != null && LocalDateTime.now().isBefore(user.getLockedUntil())) {
            long minutesLeft = java.time.Duration.between(LocalDateTime.now(), user.getLockedUntil()).toMinutes() + 1;
            throw new AppException(ErrorCode.ACCOUNT_LOCKED, Map.of("lockedForMinutes", minutesLeft));
        }

        // Wrong password
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            int attempts = (user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts()) + 1;
            user.setFailedLoginAttempts(attempts);

            if (attempts >= loginSecurityProperties.getMaxAttempts()) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(loginSecurityProperties.getLockoutMinutes()));
                user.setFailedLoginAttempts(0);
                userRepository.save(user);
                throw new AppException(ErrorCode.ACCOUNT_LOCKED,
                        Map.of("lockedForMinutes", (long) loginSecurityProperties.getLockoutMinutes()));
            }

            userRepository.save(user);
            auditService.logFailure(AuditAction.USER_LOGIN_FAILED, AuditEntityType.USER,
                    user.getId(), "Wrong password for: " + dto.getEmail(), "Invalid credentials");
            throw new AppException(ErrorCode.INVALID_EMAIL_OR_PASSWORD);
        }

        // Successful auth — reset lockout counters + update lastLogin
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // MFA required
        if (Boolean.TRUE.equals(user.getMfaEnabled())) {
            String pendingToken = otpService.generateAndStore(user.getId());
            emailService.sendOtpEmail(user.getEmail(), user.getFirstname(), otpService.getOtp(pendingToken), user.getLanguage());
            return new LoginResult(true, pendingToken, null);
        }

        auditService.log(AuditAction.USER_LOGIN, AuditEntityType.USER,
                user.getId(), "User logged in: " + user.getEmail());
        return new LoginResult(false, null, UserMapper.toDTO(user));
    }

    // -------------------- REGISTER --------------------
    public UserResponseDTO registerUser(RegisterRequestDTO dto, Role role, Set<Permission> permissions) {
        if (!dto.getEmail().toLowerCase().endsWith(ALLOWED_DOMAIN)) {
            throw new AppException(ErrorCode.INVALID_EMAIL_DOMAIN);
        }

        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        Organisation organisation = null;
        if (dto.getOrganisationId() != null) {
            organisation = organisationRepository.findById(dto.getOrganisationId())
                    .orElseThrow(() -> new AppException(ErrorCode.ORGANISATION_NOT_FOUND));
        }

        UserGrade grade = null;
        if (dto.getGradeId() != null) {
            grade = userGradeRepository.findById(dto.getGradeId())
                    .orElseThrow(() -> new AppException(ErrorCode.GRADE_NOT_FOUND));
        }

        Map<String, Object> preferencesMap = dto.getPreferences() != null ? dto.getPreferences() : new HashMap<>();

        String tempPassword = generateTempPassword();

        User user = User.builder()
                .email(dto.getEmail())
                .firstname(dto.getFirstname())
                .lastname(dto.getLastname())
                .password(passwordEncoder.encode(tempPassword))
                .phoneNumber(dto.getPhoneNumber())
                .role(role)
                .organisation(organisation)
                .grade(grade)
                .extraPermissions(permissions)
                .preferences(preferencesMap)
                .isActive(true)
                .mustChangePassword(true)
                .build();

        User savedUser = userRepository.save(user);

        emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getFirstname(), tempPassword, savedUser.getLanguage());

        auditService.log(
                AuditAction.USER_CREATED,
                AuditEntityType.USER,
                savedUser.getId(),
                "User registered: " + savedUser.getEmail()
        );

        return UserMapper.toDTO(savedUser);
    }

    // -------------------- GET ALL USERS --------------------
    public List<UserResponseDTO> getAllUsersDTO() {
        return userRepository.findAll().stream()
                .map(UserMapper::toDTO)
                .collect(Collectors.toList());
    }

    // -------------------- UPDATE PASSWORD --------------------
    public boolean updatePassword(UpdatePasswordDTO dto) {
        Optional<User> userOpt = userRepository.findByEmail(dto.getEmail());
        if (userOpt.isEmpty()) return false;

        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);

        auditService.log(
                AuditAction.USER_UPDATED,
                AuditEntityType.USER,
                user.getId(),
                "Password changed for user: " + user.getEmail()
        );

        return true;
    }

    // -------------------- DELETE USER --------------------
    public boolean deleteUserById(String id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) return false;

        User user = userOpt.get();
        userRepository.delete(user);

        auditService.log(
                AuditAction.USER_DELETED,
                AuditEntityType.USER,
                id,
                "User deleted: " + user.getEmail()
        );

        return true;
    }

    // -------------------- UPDATE USER (ADMIN & SELF) --------------------
    public boolean updateUser(UpdateUserDTO dto) {
        Optional<User> optionalUser = userRepository.findByEmail(dto.getEmail());
        if (optionalUser.isEmpty()) return false;

        User user = optionalUser.get();

        Map<String, Object> oldValues = Map.of(
                "firstname",   user.getFirstname()   != null ? user.getFirstname()   : "",
                "lastname",    user.getLastname()     != null ? user.getLastname()    : "",
                "phoneNumber", user.getPhoneNumber()  != null ? user.getPhoneNumber() : "",
                "role",        user.getRole()         != null ? user.getRole().getName() : ""
        );

        // -------------------- BASIC INFO --------------------
        if (dto.getFirstname()    != null) user.setFirstname(dto.getFirstname());
        if (dto.getLastname()     != null) user.setLastname(dto.getLastname());
        if (dto.getLanguage()     != null) user.setLanguage(dto.getLanguage());
        if (dto.getPassword()     != null) user.setPassword(passwordEncoder.encode(dto.getPassword()));
        if (dto.getPhoneNumber()  != null) user.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getPreferences()  != null) user.setPreferences(dto.getPreferences());

        // -------------------- ROLE --------------------
        if (dto.getRole() != null) {
            Role role = roleRepository.findByName(dto.getRole())
                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
            user.setRole(role);
        }

        // -------------------- PERMISSIONS --------------------
        if (dto.getPermissions() != null) {
            Set<Permission> permissions = dto.getPermissions().stream()
                    .map(name -> permissionRepository.findByName(name)
                            .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_FOUND)))
                    .collect(Collectors.toSet());
            user.setExtraPermissions(permissions);
        }

        // -------------------- GRADE --------------------
        if (dto.getGradeId() != null) {
            UserGrade grade = userGradeRepository.findById(dto.getGradeId())
                    .orElseThrow(() -> new AppException(ErrorCode.GRADE_NOT_FOUND));
            user.setGrade(grade);
        }

        userRepository.save(user);

        Map<String, Object> newValues = Map.of(
                "firstname",   user.getFirstname()   != null ? user.getFirstname()   : "",
                "lastname",    user.getLastname()     != null ? user.getLastname()    : "",
                "phoneNumber", user.getPhoneNumber()  != null ? user.getPhoneNumber() : "",
                "role",        user.getRole()         != null ? user.getRole().getName() : ""
        );

        auditService.log(
                AuditAction.USER_UPDATED,
                AuditEntityType.USER,
                user.getId(),
                "User updated: " + user.getEmail(),
                oldValues,
                newValues
        );

        return true;
    }

    // -------------------- REMOVE ROLE --------------------
    public void removeUserRole(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String oldRole = user.getRole() != null ? user.getRole().getName() : "none";

        Role defaultRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        user.getExtraPermissions().clear();
        user.setRole(defaultRole);
        userRepository.save(user);

        auditService.log(
                AuditAction.USER_UPDATED,
                AuditEntityType.USER,
                user.getId(),
                "Role removed from user: " + email + " (was: " + oldRole + ", reset to: USER)",
                Map.of("role", oldRole, "permissions", "cleared"),
                Map.of("role", "USER", "permissions", "none")
        );
    }

    // -------------------- REMOVE PERMISSIONS --------------------
    public void removeSpecificUserPermissions(String email, Set<String> permissionNames,
                                              PermissionRepository permissionRepository) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Set<Permission> toRemove = permissionNames.stream()
                .map(name -> permissionRepository.findByName(name)
                        .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_FOUND)))
                .collect(Collectors.toSet());

        user.getExtraPermissions().removeAll(toRemove);
        userRepository.save(user);

        auditService.log(
                AuditAction.USER_UPDATED,
                AuditEntityType.USER,
                user.getId(),
                "Permissions removed from user: " + email + " — removed: " + String.join(", ", permissionNames)
        );
    }

    // -------------------- TOGGLE MFA --------------------
    public UserResponseDTO toggleMfa(String id, boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        user.setMfaEnabled(enabled);
        userRepository.save(user);

        auditService.log(AuditAction.USER_UPDATED, AuditEntityType.USER,
                user.getId(), "MFA " + (enabled ? "enabled" : "disabled") + " for: " + user.getEmail());

        return UserMapper.toDTO(user);
    }

    // -------------------- TOGGLE ACTIVE STATUS --------------------
    public UserResponseDTO toggleStatus(String id, boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        user.setIsActive(active);
        userRepository.save(user);

        auditService.log(
                AuditAction.USER_UPDATED,
                AuditEntityType.USER,
                user.getId(),
                "User account " + (active ? "activated" : "deactivated") + ": " + user.getEmail()
        );

        return UserMapper.toDTO(user);
    }

    // -------------------- CHANGE GRADE --------------------
    public boolean changeUserGrade(String email, String gradeId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String oldGrade = user.getGrade() != null ? user.getGrade().getName() : "none";

        UserGrade grade = userGradeRepository.findById(gradeId)
                .orElseThrow(() -> new AppException(ErrorCode.GRADE_NOT_FOUND));

        user.setGrade(grade);
        userRepository.save(user);

        auditService.log(
                AuditAction.USER_UPDATED,
                AuditEntityType.USER,
                user.getId(),
                "Grade changed for user: " + email,
                Map.of("grade", oldGrade),
                Map.of("grade", grade.getName())
        );

        return true;
    }

    // -------------------- FORGOT PASSWORD --------------------
    @Transactional
    public void forgotPassword(String email) {
        // Silently no-op if user not found — never leak user existence
        userRepository.findByEmail(email).ifPresent(user -> {
            PasswordResetToken token = PasswordResetToken.builder()
                    .userId(user.getId())
                    .expiresAt(LocalDateTime.now().plusMinutes(30))
                    .build();
            passwordResetTokenRepository.save(token);
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstname(), token.getId(), user.getLanguage());
        });
    }

    // -------------------- RESET PASSWORD --------------------
    @Transactional
    public void resetPassword(String tokenId, String newPassword) {
        PasswordResetToken token = passwordResetTokenRepository
                .findByIdAndUsedFalseAndCancelledFalse(tokenId)
                .orElseThrow(() -> new AppException(ErrorCode.RESET_TOKEN_NOT_FOUND));

        if (LocalDateTime.now().isAfter(token.getExpiresAt())) {
            throw new AppException(ErrorCode.RESET_TOKEN_EXPIRED);
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);

        token.setUsed(true);
        passwordResetTokenRepository.save(token);

        // Revoke all sessions — force re-login on all devices
        userSessionRepository.revokeAllByUserId(user.getId());

        emailService.sendPasswordResetConfirmation(user.getEmail(), user.getFirstname(), user.getLanguage());

        auditService.log(AuditAction.USER_UPDATED, AuditEntityType.USER,
                user.getId(), "Password reset via token for: " + user.getEmail());
    }

    // -------------------- CANCEL RESET --------------------
    @Transactional
    public void cancelReset(String tokenId) {
        PasswordResetToken token = passwordResetTokenRepository
                .findByIdAndUsedFalseAndCancelledFalse(tokenId)
                .orElseThrow(() -> new AppException(ErrorCode.RESET_TOKEN_NOT_FOUND));

        token.setCancelled(true);
        passwordResetTokenRepository.save(token);

        userRepository.findById(token.getUserId()).ifPresent(user ->
                emailService.sendResetCancelledConfirmation(user.getEmail(), user.getFirstname(), user.getLanguage())
        );
    }
}