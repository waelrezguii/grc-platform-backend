package com.project.grcplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ComplianceRequirementRequestDTO {
    @NotBlank private String frameworkId;
    @NotBlank private String code;
    @NotBlank private String title;
    private String description;
    private String category;
    @NotNull private Boolean mandatory;
}