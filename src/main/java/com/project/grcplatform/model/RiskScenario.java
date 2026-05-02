package com.project.grcplatform.model;

import com.project.grcplatform.base.SoftDeleteEntity;
import com.project.grcplatform.constant.PriorityLevel;
import com.project.grcplatform.constant.ScenarioStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "risk_scenarios")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RiskScenario extends SoftDeleteEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // --- Links ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "threat_id", nullable = false)
    private Threat threat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vulnerability_id")
    private Vulnerability vulnerability;

    // --- Risk scoring (1-5) ---
    @Column(nullable = false)
    private Short likelihood;

    @Column(nullable = false)
    private Short impact;

    @Column(name = "raw_risk_score", nullable = false)
    private Short rawRiskScore; // computed via inherent risk matrix

    // --- Residual risk after controls ---
    // 1=Efficace, 2=Insuffisant, 3=Inexistant
    @Column(name = "control_evaluation")
    private Short controlEvaluation;

    @Column(name = "residual_risk_score")
    private Short residualRiskScore; // computed via residual risk matrix

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(name = "treatment_plan", columnDefinition = "TEXT")
    private String treatmentPlan;

    /**
     * Detailed qualitative and/or quantitative justification of the expected impact.
     * Required before a scenario can be validated.
     */
    @Column(name = "impact_justification", columnDefinition = "TEXT")
    private String impactJustification;

    /**
     * Lessons learned from real incidents or post-incident reviews (REX).
     * Used to enrich or adjust the scenario over time.
     */
    @Column(name = "lessons_learned", columnDefinition = "TEXT")
    private String lessonsLearned;

    // ── Annual review tracking ────────────────────────────────────────────────

    @Column(name = "last_reviewed_by")
    private String lastReviewedBy;

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    /** Due date for the next mandatory annual review. */
    @Column(name = "next_review_due")
    private LocalDateTime nextReviewDue;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ScenarioStatus status = ScenarioStatus.DRAFT;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "validated_by")
    private String validatedBy;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    // ── Prioritization ────────────────────────────────────────────────────────

    /**
     * Regulatory / compliance exposure of the scenario (1=low … 4=critical).
     * Used as one of the weighted criteria in priority score calculation.
     */
    @Column(name = "regulatory_exposure")
    private Short regulatoryExposure;

    /** Computed weighted priority score (0–100). Null until first calculation. */
    @Column(name = "priority_score")
    private Double priorityScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority_level")
    private PriorityLevel priorityLevel;

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean archived = false;

    @PrePersist
    public void onCreate() {
        computeScores();
    }

    @PreUpdate
    public void onUpdate() {
        computeScores();
    }

    // Inherent risk matrix [fréquence-1][sévérité-1]
    private static final short[][] INHERENT_MATRIX = {
        {1, 1, 1, 2, 2},  // Rare
        {1, 1, 2, 2, 3},  // Peu probable
        {1, 2, 2, 3, 4},  // Possible
        {2, 2, 3, 4, 5},  // Probable
        {2, 3, 4, 5, 5},  // Quasiment certain
    };

    // Residual risk matrix [risque inhérent-1][contrôle-1]
    private static final short[][] RESIDUAL_MATRIX = {
        {1, 1, 1},  // Mineur
        {1, 2, 2},  // Notable
        {1, 2, 3},  // Fort
        {2, 3, 4},  // Très fort
        {3, 4, 5},  // Critique
    };

    private void computeScores() {
        if (likelihood != null && impact != null) {
            rawRiskScore = INHERENT_MATRIX[likelihood - 1][impact - 1];
        }
        if (rawRiskScore != null && controlEvaluation != null) {
            residualRiskScore = RESIDUAL_MATRIX[rawRiskScore - 1][controlEvaluation - 1];
        }
    }
}
