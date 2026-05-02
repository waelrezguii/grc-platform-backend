package com.project.grcplatform.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ScenarioPriorityConfigRequestDTO {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
    private Double likelihoodWeight;

    @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
    private Double impactWeight;

    @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
    private Double regulatoryExposureWeight;

    @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
    private Double cvssWeight;

    @DecimalMin("0.0") @DecimalMax("100.0")
    private Double alertThreshold;
}
