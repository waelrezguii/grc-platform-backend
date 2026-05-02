package com.project.grcplatform.mapper;

import com.project.grcplatform.dto.RiskScenarioRequestDTO;
import com.project.grcplatform.dto.RiskScenarioResponseDTO;
import com.project.grcplatform.dto.UserSummaryDTO;
import com.project.grcplatform.model.RiskScenario;

public class RiskScenarioMapper {

    public static RiskScenarioResponseDTO toDTO(RiskScenario scenario) {
        if (scenario == null) return null;

        RiskScenarioResponseDTO dto = new RiskScenarioResponseDTO();
        dto.setId(scenario.getId());
        dto.setName(scenario.getName());
        dto.setDescription(scenario.getDescription());
        dto.setAssetId(scenario.getAsset() != null ? scenario.getAsset().getId() : null);
        dto.setThreatId(scenario.getThreat() != null ? scenario.getThreat().getId() : null);
        dto.setVulnerabilityId(scenario.getVulnerability() != null ? scenario.getVulnerability().getId() : null);
        dto.setLikelihood(scenario.getLikelihood());
        dto.setImpact(scenario.getImpact());
        dto.setRawRiskScore(scenario.getRawRiskScore());
        dto.setControlEvaluation(scenario.getControlEvaluation());
        dto.setResidualRiskScore(scenario.getResidualRiskScore());
        dto.setTreatmentPlan(scenario.getTreatmentPlan());
        dto.setImpactJustification(scenario.getImpactJustification());
        dto.setLessonsLearned(scenario.getLessonsLearned());
        dto.setStatus(scenario.getStatus());
        dto.setReviewedBy(scenario.getReviewedBy());
        dto.setReviewedAt(scenario.getReviewedAt());
        dto.setValidatedBy(scenario.getValidatedBy());
        dto.setValidatedAt(scenario.getValidatedAt());
        dto.setLastReviewedBy(scenario.getLastReviewedBy());
        dto.setLastReviewedAt(scenario.getLastReviewedAt());
        dto.setNextReviewDue(scenario.getNextReviewDue());
        dto.setReviewNotes(scenario.getReviewNotes());
        dto.setRegulatoryExposure(scenario.getRegulatoryExposure());
        dto.setPriorityScore(scenario.getPriorityScore());
        dto.setPriorityLevel(scenario.getPriorityLevel());
        dto.setArchived(Boolean.TRUE.equals(scenario.getArchived()));
        dto.setCreatedBy(UserSummaryDTO.of(scenario.getOwner()));
        dto.setCreatedAt(scenario.getCreatedAt());
        dto.setUpdatedAt(scenario.getUpdatedAt());
        return dto;
    }

    public static RiskScenario toEntity(RiskScenarioRequestDTO request) {
        if (request == null) return null;

        return RiskScenario.builder()
                .name(request.getName())
                .description(request.getDescription())
                .likelihood(request.getLikelihood())
                .impact(request.getImpact())
                .controlEvaluation(request.getControlEvaluation())
                .treatmentPlan(request.getTreatmentPlan())
                .impactJustification(request.getImpactJustification())
                .lessonsLearned(request.getLessonsLearned())
                .regulatoryExposure(request.getRegulatoryExposure())
                .build();
    }

    public static void updateEntity(RiskScenario scenario, RiskScenarioRequestDTO request) {
        if (request.getName() != null)                scenario.setName(request.getName());
        if (request.getDescription() != null)         scenario.setDescription(request.getDescription());
        if (request.getLikelihood() != null)          scenario.setLikelihood(request.getLikelihood());
        if (request.getImpact() != null)              scenario.setImpact(request.getImpact());
        if (request.getControlEvaluation() != null)   scenario.setControlEvaluation(request.getControlEvaluation());
        if (request.getTreatmentPlan() != null)          scenario.setTreatmentPlan(request.getTreatmentPlan());
        if (request.getImpactJustification() != null)    scenario.setImpactJustification(request.getImpactJustification());
        if (request.getLessonsLearned() != null)          scenario.setLessonsLearned(request.getLessonsLearned());
        if (request.getRegulatoryExposure() != null)      scenario.setRegulatoryExposure(request.getRegulatoryExposure());
    }
}