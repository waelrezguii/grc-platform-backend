package com.project.grcplatform.model;

import com.project.grcplatform.base.SoftDeleteEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "impact_evaluations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ImpactEvaluation extends SoftDeleteEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vulnerability_id", nullable = false)
    private Vulnerability vulnerability;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private RiskScenario scenario;

    /**
     * Analyst-assessed exploitation context severity (1=Low, 2=Medium, 3=High, 4=Critical).
     * Captures operational factors not reflected in CVSS alone.
     */
    @Column(name = "exploitation_context", nullable = false)
    private Short exploitationContext;

    /** CVSS score captured from the linked vulnerability at evaluation time. */
    @Column(name = "cvss_score_used", precision = 4, scale = 1)
    private BigDecimal cvssScoreUsed;

    /** Asset criticality score captured from the linked asset at evaluation time. */
    @Column(name = "asset_criticality_used")
    private Short assetCriticalityUsed;

    /**
     * Computed impact score (0.0–10.0).
     * Formula: 40% CVSS + 40% asset criticality (normalised to 10) + 20% exploitation context (normalised to 10).
     */
    @Column(name = "computed_score", nullable = false, precision = 4, scale = 1)
    private BigDecimal computedScore;

    /**
     * Optional manual override of the computed score.
     * Must be accompanied by overrideJustification.
     */
    @Column(name = "override_score", precision = 4, scale = 1)
    private BigDecimal overrideScore;

    /** Documented justification for the manual override — required when overrideScore is set. */
    @Column(name = "override_justification", columnDefinition = "TEXT")
    private String overrideJustification;

    /** ID of the user who set the override. */
    @Column(name = "overridden_by")
    private String overriddenBy;

    /** Timestamp when the override was applied or last modified. */
    @Column(name = "overridden_at")
    private LocalDateTime overriddenAt;
}
