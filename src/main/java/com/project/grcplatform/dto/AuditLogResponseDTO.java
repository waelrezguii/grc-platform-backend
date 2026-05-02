package com.project.grcplatform.dto;

import com.project.grcplatform.constant.AuditAction;
import com.project.grcplatform.constant.AuditEntityType;
import com.project.grcplatform.constant.OriginType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class AuditLogResponseDTO {
    private String id;
    private String userId;
    private String userEmail;
    private AuditAction action;
    private AuditEntityType entityType;
    private String entityId;
    private String description;
    private String ipAddress;
    private String userAgent;
    private OriginType originType;
    private Map<String, Object> oldValues;
    private Map<String, Object> newValues;
    private Boolean success;
    private String errorMessage;
    private LocalDateTime createdAt;
    private String previousHash;
    private String hash;
}