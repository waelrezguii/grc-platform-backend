package com.project.grcplatform.service;

import com.project.grcplatform.constant.ErrorCode;
import com.project.grcplatform.dto.SessionResponseDTO;
import com.project.grcplatform.exception.AppException;
import com.project.grcplatform.model.User;
import com.project.grcplatform.model.UserSession;
import com.project.grcplatform.repository.UserRepository;
import com.project.grcplatform.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionService {

    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    private static final long SESSION_HOURS = 1;

    /** Called after successful login. Creates session and fires new-device/IP alerts if needed. */
    @Transactional
    public UserSession createSession(String userId, String ipAddress, String userAgent) {
        String fingerprint = fingerprint(userAgent);
        String deviceName  = userAgent != null && userAgent.length() > 120
                ? userAgent.substring(0, 120) : userAgent;

        boolean newDevice = !userSessionRepository.existsByUserIdAndDeviceFingerprint(userId, fingerprint);
        boolean newIp     = ipAddress != null && !userSessionRepository.existsByUserIdAndIpAddress(userId, ipAddress);

        UserSession session = UserSession.builder()
                .userId(userId)
                .deviceFingerprint(fingerprint)
                .deviceName(deviceName)
                .ipAddress(ipAddress)
                .expiresAt(LocalDateTime.now().plusHours(SESSION_HOURS))
                .lastActivityAt(LocalDateTime.now())
                .revoked(false)
                .build();

        userSessionRepository.save(session);

        if (newDevice || newIp) {
            sendAlerts(userId, ipAddress, deviceName, newDevice, newIp);
        }

        return session;
    }

    /** Revoke a single session (logout). */
    @Transactional
    public void revokeSession(String sessionId) {
        UserSession session = userSessionRepository.findByIdAndRevokedFalse(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));
        session.setRevoked(true);
        userSessionRepository.save(session);
    }

    /** Revoke all sessions for a user (logout everywhere). */
    @Transactional
    public void revokeAllSessions(String userId) {
        userSessionRepository.revokeAllByUserId(userId);
    }

    /** List active sessions for a user. */
    public List<SessionResponseDTO> getActiveSessions(String userId, String currentSessionId) {
        return userSessionRepository.findAllByUserIdAndRevokedFalse(userId).stream()
                .map(s -> new SessionResponseDTO(
                        s.getId(),
                        s.getDeviceName(),
                        s.getIpAddress(),
                        s.getCreatedAt() != null ? s.getCreatedAt().toString() : null,
                        s.getLastActivityAt() != null ? s.getLastActivityAt().toString() : null,
                        s.getId().equals(currentSessionId)
                ))
                .toList();
    }

    // ── Alerts ───────────────────────────────────────────────────────────────

    private void sendAlerts(String userId, String ip, String device,
                            boolean newDevice, boolean newIp) {
        userRepository.findById(userId).ifPresent(user -> {
            emailService.sendNewDeviceAlert(user, ip, device, newDevice, newIp);

            List<User> admins = userRepository.findByRole_Name("ADMIN");
            admins.stream()
                    .filter(a -> !a.getId().equals(userId))
                    .forEach(admin -> emailService.sendAdminSecurityAlert(admin, user, ip, device, newDevice, newIp));
        });
    }

    // ── Utils ─────────────────────────────────────────────────────────────────

    private String fingerprint(String userAgent) {
        if (userAgent == null) return "unknown";
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(userAgent.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return userAgent.substring(0, Math.min(64, userAgent.length()));
        }
    }
}
