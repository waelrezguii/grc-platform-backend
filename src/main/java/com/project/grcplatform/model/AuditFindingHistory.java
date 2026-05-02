package com.project.grcplatform.model;

import com.project.grcplatform.model.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "audit_finding_history", indexes = {
        @Index(name = "idx_af_history_finding_id", columnList = "finding_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditFindingHistory extends AuditableEntity {

    @Column(name = "finding_id", nullable = false)
    private String findingId;

    @Column(name = "change_summary")
    private String changeSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> snapshot;
}
