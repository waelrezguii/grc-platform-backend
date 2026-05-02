package com.project.grcplatform.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ImpactEvaluationResponseDTO {

    private String id;

    private String vulnerabilityId;
    private String vulnerabilityTitle;

    private String scenarioId;
    private String scenarioName;

    private String assetId;
    private String assetName;

    /** Analyst-assessed exploitation context (1–4). */
    private Short exploitationContext;

    /** CVSS score captured at evaluation time. */
    private BigDecimal cvssScoreUsed;

    /** Asset criticality score captured at evaluation time. */
    private Short assetCriticalityUsed;

    /** Automatically computed impact score (0.0–10.0). */
    private BigDecimal computedScore;

    /** Manual override score, if set. */
    private BigDecimal overrideScore;

    /** Justification for the override. */
    private String overrideJustification;

    /** User who applied the override. */
    private String overriddenBy;

    /** When the override was applied. */
    private LocalDateTime overriddenAt;

    /**
     * Effective score used for risk prioritisation.
     * Equals overrideScore if set, otherwise computedScore.
     */
    private BigDecimal effectiveScore;

    private UserSummaryDTO createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
