package com.project.grcplatform.controller;

import com.project.grcplatform.dto.RegisterRequestDTO;
import com.project.grcplatform.dto.UpdateUserDTO;
import com.project.grcplatform.dto.UserResponseDTO;
import com.project.grcplatform.exception.AppException;
import com.project.grcplatform.constant.ErrorCode;
import com.project.grcplatform.model.Permission;
import com.project.grcplatform.model.Role;
import com.project.grcplatform.repository.PermissionRepository;
import com.project.grcplatform.repository.RoleRepository;
import com.project.grcplatform.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    // -------------------- CREATE USER --------------------
    @PostMapping("/")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@Valid @RequestBody RegisterRequestDTO dto) {

        Role role = roleRepository.findByName(dto.getRole())
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        Set<Permission> permissions = dto.getPermissions() == null
                ? new HashSet<>()
                : dto.getPermissions().stream()
                .map(name -> permissionRepository.findByName(name)
                        .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_FOUND)))
                .collect(Collectors.toSet());

        UserResponseDTO response = userService.registerUser(dto, role, permissions);
        return ResponseEntity.ok(response);
    }

    // -------------------- GET ALL USERS --------------------
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsersDTO());
    }

    // -------------------- UPDATE USER --------------------
    @PatchMapping("/")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(@RequestBody UpdateUserDTO dto) {
        boolean updated = userService.updateUser(dto);

        if (!updated) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        return ResponseEntity.ok(Map.of("code", "SUCCESS"));
    }

    // -------------------- DELETE USER --------------------
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        boolean deleted = userService.deleteUserById(id);
        if (!deleted) throw new AppException(ErrorCode.USER_NOT_FOUND);

        return ResponseEntity.ok(Map.of(
                "code", "SUCCESS",
                "message", "User deleted successfully"
        ));
    }

    // -------------------- REMOVE ROLE --------------------
    @PostMapping("/{email}/remove-role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> removeRole(@PathVariable String email) {
        userService.removeUserRole(email);
        return ResponseEntity.ok(Map.of(
                "code", "SUCCESS",
                "message", "User role removed"
        ));
    }

    // -------------------- REMOVE PERMISSIONS --------------------
    @PostMapping("/{email}/remove-permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> removePermissions(
            @PathVariable String email,
            @RequestBody Set<String> permissionsToRemove // <- list of permission names
    ) {

        userService.removeSpecificUserPermissions(email, permissionsToRemove, permissionRepository);

        return ResponseEntity.ok(Map.of(
                "code", "SUCCESS",
                "message", "Specified permissions removed"
        ));
    }

    // -------------------- TOGGLE MFA --------------------
    @PatchMapping("/{id}/mfa")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleMfa(
            @PathVariable String id,
            @RequestParam boolean enabled
    ) {
        return ResponseEntity.ok(userService.toggleMfa(id, enabled));
    }

    // -------------------- TOGGLE STATUS --------------------
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleStatus(
            @PathVariable String id,
            @RequestParam boolean active
    ) {
        return ResponseEntity.ok(userService.toggleStatus(id, active));
    }

    // -------------------- CHANGE GRADE --------------------
    @PostMapping("/{email}/change-grade/{gradeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> changeGrade(@PathVariable String email, @PathVariable String gradeId) {
        userService.changeUserGrade(email, gradeId);
        return ResponseEntity.ok(Map.of(
                "code", "SUCCESS",
                "message", "User grade updated"
        ));
    }
}