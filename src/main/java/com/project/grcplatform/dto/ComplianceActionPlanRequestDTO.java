package com.project.grcplatform.dto;

import com.project.grcplatform.constant.ActionPlanPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ComplianceActionPlanRequestDTO {
    @NotBlank private String evaluationId;
    private String requirementId;
    @NotBlank private String title;
    private String description;
    @NotNull  private ActionPlanPriority priority;
    private String assigneeId;
    private LocalDate dueDate;
}