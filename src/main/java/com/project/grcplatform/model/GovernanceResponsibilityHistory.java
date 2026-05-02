package com.project.grcplatform.model;

import com.project.grcplatform.model.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "governance_responsibility_history", indexes = {
        @Index(name = "idx_gresp_history_resp_id", columnList = "responsibility_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GovernanceResponsibilityHistory extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsibility_id", nullable = false)
    private GovernanceResponsibility responsibility;

    @Column(name = "change_summary")
    private String changeSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> snapshot;
}