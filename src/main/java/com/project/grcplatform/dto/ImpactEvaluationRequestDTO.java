package com.project.grcplatform.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ImpactEvaluationRequestDTO {

    @NotBlank
    private String vulnerabilityId;

    @NotBlank
    private String scenarioId;

    /**
     * Analyst-assessed exploitation context severity.
     * 1 = Low, 2 = Medium, 3 = High, 4 = Critical
     */
    @NotNull
    @Min(1) @Max(4)
    private Short exploitationContext;

    /**
     * Optional manual override of the computed score (0.0–10.0).
     * Requires overrideJustification.
     */
    @Min(0) @Max(10)
    private BigDecimal overrideScore;

    /** Documented justification for the manual override. */
    private String overrideJustification;
}
