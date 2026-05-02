package com.project.grcplatform.mapper;

import com.project.grcplatform.dto.TreatmentActionRequestDTO;
import com.project.grcplatform.dto.TreatmentActionResponseDTO;
import com.project.grcplatform.dto.TreatmentPlanRequestDTO;
import com.project.grcplatform.dto.TreatmentPlanResponseDTO;
import com.project.grcplatform.dto.UserSummaryDTO;
import com.project.grcplatform.model.TreatmentAction;
import com.project.grcplatform.model.TreatmentPlan;

public class TreatmentPlanMapper {

    public static TreatmentPlanResponseDTO toDTO(TreatmentPlan plan) {
        if (plan == null) return null;

        TreatmentPlanResponseDTO dto = new TreatmentPlanResponseDTO();
        dto.setId(plan.getId());
        dto.setTitle(plan.getTitle());
        dto.setDescription(plan.getDescription());
        dto.setScenarioId(plan.getScenario() != null ? plan.getScenario().getId() : null);
        dto.setAssessmentId(plan.getAssessment() != null ? plan.getAssessment().getId() : null);
        dto.setTreatmentStrategy(plan.getTreatmentStrategy());
        dto.setStatus(plan.getStatus());
        dto.setDueDate(plan.getDueDate());
        dto.setEstimatedBudget(plan.getEstimatedBudget());
        dto.setActualCost(plan.getActualCost());
        dto.setProgressPct(plan.getProgressPct());
        dto.setTreatmentEffectiveness(plan.getTreatmentEffectiveness());
        dto.setTargetRiskScore(plan.getTargetRiskScore());
        dto.setArchived(Boolean.TRUE.equals(plan.getArchived()));
        dto.setCreatedBy(UserSummaryDTO.of(plan.getCreatedBy()));
        dto.setCreatedAt(plan.getCreatedAt());
        dto.setUpdatedAt(plan.getUpdatedAt());
        return dto;
    }

    public static TreatmentPlan toEntity(TreatmentPlanRequestDTO request) {
        if (request == null) return null;

        return TreatmentPlan.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .treatmentStrategy(request.getTreatmentStrategy())
                .dueDate(request.getDueDate())
                .estimatedBudget(request.getEstimatedBudget())
                .progressPct(request.getProgressPct() != null ? request.getProgressPct() : 0)
                .treatmentEffectiveness(request.getTreatmentEffectiveness())
                .build();
    }

    public static void updateEntity(TreatmentPlan plan, TreatmentPlanRequestDTO request) {
        if (request.getTitle() != null)              plan.setTitle(request.getTitle());
        if (request.getDescription() != null)        plan.setDescription(request.getDescription());
        if (request.getTreatmentStrategy() != null)  plan.setTreatmentStrategy(request.getTreatmentStrategy());
        if (request.getDueDate() != null)            plan.setDueDate(request.getDueDate());
        if (request.getEstimatedBudget() != null)    plan.setEstimatedBudget(request.getEstimatedBudget());
        if (request.getProgressPct() != null)             plan.setProgressPct(request.getProgressPct());
        if (request.getTreatmentEffectiveness() != null)  plan.setTreatmentEffectiveness(request.getTreatmentEffectiveness());
    }

    public static TreatmentActionResponseDTO toActionDTO(TreatmentAction action) {
        if (action == null) return null;

        TreatmentActionResponseDTO dto = new TreatmentActionResponseDTO();
        dto.setId(action.getId());
        dto.setPlanId(action.getPlan() != null ? action.getPlan().getId() : null);
        dto.setTitle(action.getTitle());
        dto.setDescription(action.getDescription());
        dto.setPriority(action.getPriority());
        dto.setStatus(action.getStatus());
        dto.setAssigneeId(action.getAssignee() != null ? action.getAssignee().getId() : null);
        dto.setDueDate(action.getDueDate());
        dto.setCompletedAt(action.getCompletedAt());
        dto.setNotes(action.getNotes());
        dto.setCreatedBy(action.getCreatedBy() != null ? action.getCreatedBy().getId() : null);
        dto.setCreatedAt(action.getCreatedAt());
        dto.setUpdatedAt(action.getUpdatedAt());
        return dto;
    }

    public static TreatmentAction toActionEntity(TreatmentActionRequestDTO request, String planId) {
        if (request == null) return null;

        return TreatmentAction.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : com.project.grcplatform.constant.ActionPriority.MEDIUM)
                .dueDate(request.getDueDate())
                .notes(request.getNotes())
                .build();
    }

    public static void updateActionEntity(TreatmentAction action, TreatmentActionRequestDTO request) {
        if (request.getTitle() != null)       action.setTitle(request.getTitle());
        if (request.getDescription() != null) action.setDescription(request.getDescription());
        if (request.getPriority() != null)    action.setPriority(request.getPriority());
        if (request.getDueDate() != null)     action.setDueDate(request.getDueDate());
        if (request.getNotes() != null)       action.setNotes(request.getNotes());
    }
}