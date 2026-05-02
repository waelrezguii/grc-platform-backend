package com.project.grcplatform.mapper;

import com.project.grcplatform.dto.ComplianceActionPlanRequestDTO;
import com.project.grcplatform.dto.ComplianceActionPlanResponseDTO;
import com.project.grcplatform.dto.UserSummaryDTO;
import com.project.grcplatform.model.ComplianceActionPlan;

public class ComplianceActionPlanMapper {
    private ComplianceActionPlanMapper() {}

    public static ComplianceActionPlanResponseDTO toDTO(ComplianceActionPlan p) {
        return ComplianceActionPlanResponseDTO.builder()
                .id(p.getId()).evaluationId(p.getEvaluation() != null ? p.getEvaluation().getId() : null).requirementId(p.getRequirement() != null ? p.getRequirement().getId() : null)
                .title(p.getTitle()).description(p.getDescription()).priority(p.getPriority())
                .status(p.getStatus()).assigneeId(p.getAssignee() != null ? p.getAssignee().getId() : null).dueDate(p.getDueDate())
                .completedAt(p.getCompletedAt()).ownerId(p.getOwner() != null ? p.getOwner().getId() : null)
                .createdBy(UserSummaryDTO.of(p.getOwner())).createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt())
                .build();
    }

    public static ComplianceActionPlan toEntity(ComplianceActionPlanRequestDTO r) {
        return ComplianceActionPlan.builder()
                .title(r.getTitle()).description(r.getDescription()).priority(r.getPriority())
                .dueDate(r.getDueDate())
                .build();
    }

    public static void updateEntity(ComplianceActionPlan p, ComplianceActionPlanRequestDTO r) {
        if (r.getTitle() != null)       p.setTitle(r.getTitle());
        if (r.getDescription() != null) p.setDescription(r.getDescription());
        if (r.getPriority() != null)    p.setPriority(r.getPriority());
        if (r.getDueDate() != null)     p.setDueDate(r.getDueDate());
    }
}