package com.project.grcplatform.mapper;

import com.project.grcplatform.dto.AuditLogResponseDTO;
import com.project.grcplatform.model.AuditLog;

public class AuditLogMapper {

    public static AuditLogResponseDTO toDTO(AuditLog log) {
        if (log == null) return null;

        AuditLogResponseDTO dto = new AuditLogResponseDTO();
        dto.setId(log.getId());
        dto.setUserId(log.getUserId());
        dto.setUserEmail(log.getUserEmail());
        dto.setAction(log.getAction());
        dto.setEntityType(log.getEntityType());
        dto.setEntityId(log.getEntityId());
        dto.setDescription(log.getDescription());
        dto.setIpAddress(log.getIpAddress());
        dto.setUserAgent(log.getUserAgent());
        dto.setOriginType(log.getOriginType());
        dto.setOldValues(log.getOldValues());
        dto.setNewValues(log.getNewValues());
        dto.setSuccess(log.getSuccess());
        dto.setErrorMessage(log.getErrorMessage());
        dto.setCreatedAt(log.getCreatedAt());
        dto.setPreviousHash(log.getPreviousHash());
        dto.setHash(log.getHash());
        return dto;
    }
}