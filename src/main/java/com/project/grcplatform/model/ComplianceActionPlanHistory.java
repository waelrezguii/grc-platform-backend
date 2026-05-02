package com.project.grcplatform.model;

import com.project.grcplatform.model.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "compliance_action_plan_history", indexes = {
        @Index(name = "idx_cap_history_plan_id", columnList = "action_plan_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ComplianceActionPlanHistory extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_plan_id", nullable = false)
    private ComplianceActionPlan actionPlan;

    @Column(name = "change_summary")
    private String changeSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> snapshot;
}