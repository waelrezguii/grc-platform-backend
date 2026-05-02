package com.project.grcplatform.dto;

import com.project.grcplatform.constant.TreatmentStrategy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RiskAssessmentRequestDTO {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "Organisation is required")
    private String organisationId;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;

    private String riskOwnerId;

    private TreatmentStrategy treatmentStrategy;

    private String treatmentNotes;
}