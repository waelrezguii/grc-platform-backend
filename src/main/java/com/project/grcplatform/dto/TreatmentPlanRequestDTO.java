package com.project.grcplatform.dto;

import com.project.grcplatform.constant.TreatmentStrategy;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TreatmentPlanRequestDTO {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "Scenario is required")
    private String scenarioId;

    private String assessmentId;

    @NotNull(message = "Treatment strategy is required")
    private TreatmentStrategy treatmentStrategy;

    private LocalDate dueDate;

    private BigDecimal estimatedBudget;

    @Min(0) @Max(100)
    private Short progressPct;

    // 1=Efficace, 2=Insuffisant, 3=Inexistant
    @Min(1) @Max(3)
    private Short treatmentEffectiveness;
}