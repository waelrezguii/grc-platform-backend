package com.project.grcplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ComplianceEvaluationRequestDTO {
    @NotBlank private String frameworkId;
    @NotBlank private String title;
    private String description;
    private String scope;
    private String evaluatorId;
    private LocalDate startDate;
    private LocalDate endDate;
}