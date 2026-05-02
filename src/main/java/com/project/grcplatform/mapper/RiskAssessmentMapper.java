package com.project.grcplatform.mapper;

import com.project.grcplatform.dto.RiskAssessmentRequestDTO;
import com.project.grcplatform.dto.RiskAssessmentResponseDTO;
import com.project.grcplatform.dto.UserSummaryDTO;
import com.project.grcplatform.model.RiskAssessment;

public class RiskAssessmentMapper {

    public static RiskAssessmentResponseDTO toDTO(RiskAssessment assessment) {
        if (assessment == null) return null;

        RiskAssessmentResponseDTO dto = new RiskAssessmentResponseDTO();
        dto.setId(assessment.getId());
        dto.setTitle(assessment.getTitle());
        dto.setDescription(assessment.getDescription());
        dto.setOrganisationId(assessment.getOrganisation() != null ? assessment.getOrganisation().getId() : null);
        dto.setStartDate(assessment.getStartDate());
        dto.setEndDate(assessment.getEndDate());
        dto.setStatus(assessment.getStatus());
        dto.setOverallRiskScore(assessment.getOverallRiskScore());
        dto.setOverallResidualScore(assessment.getOverallResidualScore());
        dto.setTreatmentStrategy(assessment.getTreatmentStrategy());
        dto.setTreatmentNotes(assessment.getTreatmentNotes());
        dto.setRiskOwnerId(assessment.getRiskOwner() != null ? assessment.getRiskOwner().getId() : null);
        dto.setReviewedBy(assessment.getReviewedBy());
        dto.setReviewedAt(assessment.getReviewedAt());
        dto.setApprovedBy(assessment.getApprovedBy());
        dto.setApprovedAt(assessment.getApprovedAt());
        dto.setCreatedBy(UserSummaryDTO.of(assessment.getOwner()));
        dto.setCreatedAt(assessment.getCreatedAt());
        dto.setUpdatedAt(assessment.getUpdatedAt());
        return dto;
    }

    public static RiskAssessment toEntity(RiskAssessmentRequestDTO request) {
        if (request == null) return null;

        return RiskAssessment.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .treatmentStrategy(request.getTreatmentStrategy())
                .treatmentNotes(request.getTreatmentNotes())
                .build();
    }

    public static void updateEntity(RiskAssessment assessment, RiskAssessmentRequestDTO request) {
        if (request.getTitle() != null)             assessment.setTitle(request.getTitle());
        if (request.getDescription() != null)       assessment.setDescription(request.getDescription());
        if (request.getStartDate() != null)         assessment.setStartDate(request.getStartDate());
        if (request.getEndDate() != null)           assessment.setEndDate(request.getEndDate());
        if (request.getTreatmentStrategy() != null) assessment.setTreatmentStrategy(request.getTreatmentStrategy());
        if (request.getTreatmentNotes() != null)    assessment.setTreatmentNotes(request.getTreatmentNotes());
    }
}