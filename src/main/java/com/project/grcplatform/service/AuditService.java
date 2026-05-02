package com.project.grcplatform.service;

import com.project.grcplatform.constant.AuditAction;
import com.project.grcplatform.constant.AuditEntityType;
import com.project.grcplatform.constant.OriginType;
import com.project.grcplatform.model.AuditLog;
import com.project.grcplatform.repository.AuditLogRepository;
import com.project.grcplatform.repository.UserRepository;
import com.project.grcplatform.security.JwtAuthToken;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    /**
     * Log a successful action — origin is auto-detected (MANUAL vs API).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AuditAction action, AuditEntityType entityType,
                    String entityId, String description) {
        saveLog(action, entityType, entityId, description, null, null, true, null, detectOrigin());
    }

    /**
     * Log with before/after values — use for updates. Origin is auto-detected.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AuditAction action, AuditEntityType entityType,
                    String entityId, String description,
                    Map<String, Object> oldValues, Map<String, Object> newValues) {
        saveLog(action, entityType, entityId, description, oldValues, newValues, true, null, detectOrigin());
    }

    /**
     * Log a failed action. Origin is auto-detected.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailure(AuditAction action, AuditEntityType entityType,
                           String entityId, String description, String errorMessage) {
        saveLog(action, entityType, entityId, description, null, null, false, errorMessage, detectOrigin());
    }

    /**
     * Log an action triggered by a scheduled batch job.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logBatch(AuditAction action, AuditEntityType entityType,
                         String entityId, String description) {
        saveLog(action, entityType, entityId, description, null, null, true, null, OriginType.BATCH);
    }

    // -------------------------

    private void saveLog(AuditAction action, AuditEntityType entityType,
                         String entityId, String description,
                         Map<String, Object> oldValues, Map<String, Object> newValues,
                         boolean success, String errorMessage, OriginType originType) {
        try {
            String userId = getCurrentUserId();
            String userEmail = resolveUserEmail(userId);
            String ipAddress = resolveIpAddress();
            String userAgent = resolveUserAgent();
            String now = LocalDateTime.now().toString();

            String previousHash = auditLogRepository.findTopByOrderByCreatedAtDesc()
                    .map(AuditLog::getHash)
                    .orElse("GENESIS");

            String hash = sha256(previousHash + "|" + userId + "|" + action.name()
                    + "|" + entityType.name() + "|" + nullSafe(entityId) + "|" + now);

            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .userEmail(userEmail)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .description(description)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .oldValues(oldValues)
                    .newValues(newValues)
                    .success(success)
                    .errorMessage(errorMessage)
                    .originType(originType)
                    .previousHash(previousHash)
                    .hash(hash)
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            // Never let audit logging break the main flow
            log.error("Failed to save audit log for action {}: {}", action, e.getMessage());
        }
    }

    /**
     * Verifies the integrity of the entire audit chain.
     * Returns the ID of the first broken link, or null if the chain is intact.
     */
    public String verifyChain() {
        List<AuditLog> entries = auditLogRepository.findAllByOrderByCreatedAtAsc();
        if (entries.isEmpty()) return null;

        String expectedPreviousHash = "GENESIS";
        for (AuditLog entry : entries) {
            if (!MessageDigest.isEqual(expectedPreviousHash.getBytes(StandardCharsets.UTF_8), entry.getPreviousHash().getBytes(StandardCharsets.UTF_8))) {
                return entry.getId();
            }
            expectedPreviousHash = entry.getHash();
        }
        return null;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    /**
     * If there is an active HTTP request, determine origin from the User-Agent header:
     * - Requests from known API clients (no browser UA) → API
     * - All others (browser / manual) → MANUAL
     * If no HTTP request is present (scheduler context) → BATCH
     */
    private OriginType detectOrigin() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return OriginType.BATCH;
            String ua = attrs.getRequest().getHeader("User-Agent");
            if (ua == null || ua.isBlank()) return OriginType.API;
            String uaLower = ua.toLowerCase();
            // Browsers always include "mozilla" in their UA string
            if (uaLower.contains("mozilla") || uaLower.contains("chrome")
                    || uaLower.contains("safari") || uaLower.contains("firefox")) {
                return OriginType.MANUAL;
            }
            return OriginType.API;
        } catch (Exception e) {
            return OriginType.MANUAL;
        }
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthToken jwtAuthToken) {
            return jwtAuthToken.getUserId();
        }
        return "system";
    }

    private String resolveUserEmail(String userId) {
        if (userId == null || "system".equals(userId)) return "system";
        try {
            return userRepository.findById(userId)
                    .map(u -> u.getEmail())
                    .orElse("unknown");
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String resolveIpAddress() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest request = attrs.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveUserAgent() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            return attrs.getRequest().getHeader("User-Agent");
        } catch (Exception e) {
            return null;
        }
    }
}