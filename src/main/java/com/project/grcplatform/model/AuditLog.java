package com.project.grcplatform.model;

import com.project.grcplatform.base.BaseEntity;
import com.project.grcplatform.constant.AuditAction;
import com.project.grcplatform.constant.AuditEntityType;
import com.project.grcplatform.constant.OriginType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_user_id",    columnList = "user_id"),
        @Index(name = "idx_audit_entity",     columnList = "entity_type, entity_id"),
        @Index(name = "idx_audit_action",     columnList = "action"),
        @Index(name = "idx_audit_created_at", columnList = "created_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog extends BaseEntity {

    // Who performed the action
    @Column(name = "user_id")
    private String userId;

    @Column(name = "user_email")
    private String userEmail;

    // What they did
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    // On which entity
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    private AuditEntityType entityType;

    @Column(name = "entity_id")
    private String entityId;

    // Human readable description
    @Column(nullable = false)
    private String description;

    // How the action was triggered
    @Enumerated(EnumType.STRING)
    @Column(name = "origin_type", nullable = false)
    @Builder.Default
    private OriginType originType = OriginType.MANUAL;

    // Request metadata
    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    // Optional before/after diff
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_values", columnDefinition = "jsonb")
    private Map<String, Object> oldValues;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_values", columnDefinition = "jsonb")
    private Map<String, Object> newValues;

    // Outcome
    @Column(nullable = false)
    @Builder.Default
    private Boolean success = true;

    @Column(name = "error_message")
    private String errorMessage;

    // Tamper-detection chain
    @Column(name = "previous_hash", nullable = false, updatable = false)
    private String previousHash;

    @Column(name = "hash", nullable = false, updatable = false)
    private String hash;
}
