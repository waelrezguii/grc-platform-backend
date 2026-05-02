package com.project.grcplatform.dto;

import com.project.grcplatform.constant.NotificationEntityType;
import com.project.grcplatform.constant.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponseDTO {

    private String id;
    private String recipientId;
    private NotificationType type;
    private String title;
    private String message;
    private NotificationEntityType entityType;
    private String entityId;
    private Boolean read;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}