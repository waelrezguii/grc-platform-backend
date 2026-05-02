package com.project.grcplatform.model;

import com.project.grcplatform.base.SoftDeleteEntity;
import com.project.grcplatform.constant.TreatmentPlanStatus;
import com.project.grcplatform.constant.TreatmentStrategy;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "treatment_plans")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TreatmentPlan extends SoftDeleteEntity {

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Linked to a risk scenario
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private RiskScenario scenario;

    // Linked to the assessment it belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id")
    private RiskAssessment assessment;

    @Enumerated(EnumType.STRING)
    @Column(name = "treatment_strategy", nullable = false)
    private TreatmentStrategy treatmentStrategy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TreatmentPlanStatus status = TreatmentPlanStatus.DRAFT;

    @Column(name = "due_date")
    private LocalDate dueDate;

    // Budget tracking
    @Column(name = "estimated_budget", precision = 12, scale = 2)
    private BigDecimal estimatedBudget;

    @Column(name = "actual_cost", precision = 12, scale = 2)
    private BigDecimal actualCost;

    // Progress 0-100
    @Column(name = "progress_pct")
    @Builder.Default
    private Short progressPct = 0;

    // 1=Efficace, 2=Insuffisant, 3=Inexistant — expected effectiveness of treatment actions
    @Column(name = "treatment_effectiveness")
    private Short treatmentEffectiveness;

    // Risque cible = matrix(residualRiskScore, treatmentEffectiveness) — computed in service
    @Column(name = "target_risk_score")
    private Short targetRiskScore;

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean archived = false;
}
