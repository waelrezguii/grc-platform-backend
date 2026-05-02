package com.project.grcplatform.controller;

import com.project.grcplatform.constant.ErrorCode;
import com.project.grcplatform.dto.*;
import com.project.grcplatform.exception.AppException;
import com.project.grcplatform.mapper.UserMapper;
import com.project.grcplatform.repository.UserRepository;
import com.project.grcplatform.security.JwtAuthToken;
import com.project.grcplatform.security.JwtUtils;
import com.project.grcplatform.service.OtpService;
import com.project.grcplatform.service.SessionService;
import com.project.grcplatform.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final OtpService otpService;
    private final SessionService sessionService;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    // ── Login ─────────────────────────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO dto,
                                   HttpServletRequest request) {
        LoginResult result = userService.login(dto);

        if (result.mfaRequired()) {
            return ResponseEntity.ok(Map.of(
                    "mfaRequired", true,
                    "pendingToken", result.pendingToken()
            ));
        }

        return buildTokenResponse(result.user(), request);
    }

    // ── MFA validate ──────────────────────────────────────────────────────────

    @PostMapping("/mfa/validate")
    public ResponseEntity<?> validateMfa(@Valid @RequestBody MfaValidateRequestDTO dto,
                                         HttpServletRequest request) {
        String userId = otpService.validateAndConsume(dto.getPendingToken(), dto.getOtp());

        UserResponseDTO user = userRepository.findById(userId)
                .map(UserMapper::toDTO)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return buildTokenResponse(user, request);
    }

    // ── Logout (current session) ──────────────────────────────────────────────

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        JwtAuthToken auth = (JwtAuthToken) SecurityContextHolder.getContext().getAuthentication();
        sessionService.revokeSession(auth.getSessionId());
        return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "Logged out successfully"));
    }

    // ── Logout everywhere ─────────────────────────────────────────────────────

    @DeleteMapping("/sessions")
    public ResponseEntity<?> logoutAll() {
        JwtAuthToken auth = (JwtAuthToken) SecurityContextHolder.getContext().getAuthentication();
        sessionService.revokeAllSessions(auth.getUserId());
        return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "All sessions revoked"));
    }

    // ── List active sessions ──────────────────────────────────────────────────

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionResponseDTO>> getSessions() {
        JwtAuthToken auth = (JwtAuthToken) SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(sessionService.getActiveSessions(auth.getUserId(), auth.getSessionId()));
    }

    // ── Change password (first login) ─────────────────────────────────────────

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody UpdatePasswordDTO dto) {
        boolean updated = userService.updatePassword(dto);
        if (!updated) throw new AppException(ErrorCode.USER_NOT_FOUND);
        return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "Password changed successfully"));
    }

    // ── Forgot password ───────────────────────────────────────────────────────

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO dto) {
        userService.forgotPassword(dto.getEmail());
        // Always same response — never leak whether email exists
        return ResponseEntity.ok(Map.of("code", "SUCCESS",
                "message", "If this email is registered, a reset link has been sent"));
    }

    // ── Reset password ────────────────────────────────────────────────────────

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO dto) {
        userService.resetPassword(dto.getToken(), dto.getNewPassword());
        return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "Password reset successfully"));
    }

    // ── Cancel reset (clicked from email) ────────────────────────────────────

    @GetMapping("/cancel-reset")
    public ResponseEntity<?> cancelReset(@RequestParam String token) {
        userService.cancelReset(token);
        return ResponseEntity.ok(Map.of("code", "SUCCESS",
                "message", "Password reset cancelled. Your password remains unchanged."));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private ResponseEntity<?> buildTokenResponse(UserResponseDTO user, HttpServletRequest request) {
        String ip        = resolveIp(request);
        String userAgent = request.getHeader("User-Agent");

        var session = sessionService.createSession(user.getId(), ip, userAgent);
        String token = jwtUtils.generateToken(user.getId(), user.getRole(), session.getId());

        return ResponseEntity.ok(Map.of(
                "code",  "SUCCESS",
                "message", "Login successful",
                "token", token,
                "user",  user
        ));
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
