package com.project.grcplatform.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RiskScenarioRequestDTO {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotBlank(message = "Asset is required")
    private String assetId;

    @NotBlank(message = "Threat is required")
    private String threatId;

    private String vulnerabilityId;

    @NotNull(message = "Likelihood is required")
    @Min(1) @Max(5)
    private Short likelihood;

    @NotNull(message = "Impact is required")
    @Min(1) @Max(5)
    private Short impact;

    // 1=Efficace, 2=Insuffisant, 3=Inexistant
    @Min(1) @Max(3)
    private Short controlEvaluation;

    private String treatmentPlan;
    private String impactJustification;
    private String lessonsLearned;

    @Min(1) @Max(4)
    private Short regulatoryExposure;
}