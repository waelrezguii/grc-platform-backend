package com.project.grcplatform.dto;

import com.project.grcplatform.constant.ComplianceLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ComplianceEvaluationItemRequestDTO {
    @NotBlank private String requirementId;
    @NotNull  private ComplianceLevel complianceLevel;
    private String comment;
    private String evidence;
}