package com.project.grcplatform.mapper;

import com.project.grcplatform.dto.NotificationResponseDTO;
import com.project.grcplatform.model.Notification;

public class NotificationMapper {

    private NotificationMapper() {}

    public static NotificationResponseDTO toDTO(Notification n) {
        return NotificationResponseDTO.builder()
                .id(n.getId())
                .recipientId(n.getRecipient() != null ? n.getRecipient().getId() : null)
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .entityType(n.getEntityType())
                .entityId(n.getEntityId())
                .read(n.getRead())
                .readAt(n.getReadAt())
                .createdAt(n.getCreatedAt())
                .build();
    }
}