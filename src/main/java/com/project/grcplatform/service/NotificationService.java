package com.project.grcplatform.service;

import com.project.grcplatform.constant.ErrorCode;
import com.project.grcplatform.constant.NotificationEntityType;
import com.project.grcplatform.constant.NotificationType;
import com.project.grcplatform.dto.NotificationResponseDTO;
import com.project.grcplatform.exception.AppException;
import com.project.grcplatform.mapper.NotificationMapper;
import com.project.grcplatform.model.Notification;
import com.project.grcplatform.repository.NotificationRepository;
import com.project.grcplatform.repository.UserRepository;
import com.project.grcplatform.security.JwtAuthToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // ─── Public read API ────────────────────────────────────────────────────────

    public Page<NotificationResponseDTO> getMyNotifications(Boolean read, Pageable pageable) {
        String userId = getCurrentUserId();
        return notificationRepository
                .findByRecipientId(userId, read, pageable)
                .map(NotificationMapper::toDTO);
    }

    public long getUnreadCount() {
        return notificationRepository.countByRecipient_IdAndReadFalse(getCurrentUserId());
    }

    // ─── Write API ──────────────────────────────────────────────────────────────

    @Transactional
    public NotificationResponseDTO markAsRead(String id) {
        String userId = getCurrentUserId();
        Notification n = notificationRepository.findByIdAndRecipient_Id(id, userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!n.getRead()) {
            n.setRead(true);
            n.setReadAt(LocalDateTime.now());
            notificationRepository.save(n);
        }
        return NotificationMapper.toDTO(n);
    }

    @Transactional
    public void markAllAsRead() {
        notificationRepository.markAllAsRead(getCurrentUserId(), LocalDateTime.now());
    }

    @Transactional
    public void delete(String id) {
        String userId = getCurrentUserId();
        Notification n = notificationRepository.findByIdAndRecipient_Id(id, userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));
        notificationRepository.delete(n);
    }

    // ─── Internal trigger API (called from other services) ──────────────────────
    // Uses REQUIRES_NEW so the notification is always saved even if the caller
    // rolls back — same pattern as AuditService.

    /**
     * Send a notification to a single recipient.
     * Safe to call from any service — never throws, never rolls back the caller.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notify(String recipientId,
                       NotificationType type,
                       NotificationEntityType entityType,
                       String entityId,
                       String title,
                       String message) {
        if (recipientId == null || recipientId.isBlank()) return;
        try {
            Notification n = Notification.builder()
                    .recipient(userRepository.getReferenceById(recipientId))
                    .type(type)
                    .entityType(entityType)
                    .entityId(entityId)
                    .title(title)
                    .message(message)
                    .build();
            notificationRepository.save(n);
        } catch (Exception e) {
            log.error("Failed to create notification for recipient={} type={}: {}", recipientId, type, e.getMessage());
        }
    }

    /**
     * Send the same notification to multiple recipients.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyMultiple(List<String> recipientIds,
                               NotificationType type,
                               NotificationEntityType entityType,
                               String entityId,
                               String title,
                               String message) {
        if (recipientIds == null || recipientIds.isEmpty()) return;
        recipientIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .forEach(recipientId -> notify(recipientId, type, entityType, entityId, title, message));
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthToken jwtAuthToken) {
            return jwtAuthToken.getUserId();
        }
        return "system";
    }
}