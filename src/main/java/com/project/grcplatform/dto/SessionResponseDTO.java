package com.project.grcplatform.dto;

public record SessionResponseDTO(
        String id,
        String deviceName,
        String ipAddress,
        String createdAt,
        String lastActivityAt,
        boolean current
) {}
