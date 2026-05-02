package com.project.grcplatform.dto;

import com.project.grcplatform.constant.ActionPlanPriority;
import com.project.grcplatform.constant.ActionPlanStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder
public class ComplianceActionPlanResponseDTO {
    private String id;
    private String evaluationId;
    private String requirementId;
    private String title;
    private String description;
    private ActionPlanPriority priority;
    private ActionPlanStatus status;
    private String assigneeId;
    private LocalDate dueDate;
    private LocalDateTime completedAt;
    private String ownerId;
    private UserSummaryDTO createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}