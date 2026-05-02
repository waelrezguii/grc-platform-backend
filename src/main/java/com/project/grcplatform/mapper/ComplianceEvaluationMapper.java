package com.project.grcplatform.mapper;

import com.project.grcplatform.dto.ComplianceEvaluationRequestDTO;
import com.project.grcplatform.dto.ComplianceEvaluationResponseDTO;
import com.project.grcplatform.dto.UserSummaryDTO;
import com.project.grcplatform.model.ComplianceEvaluation;

public class ComplianceEvaluationMapper {
    private ComplianceEvaluationMapper() {}

    public static ComplianceEvaluationResponseDTO toDTO(ComplianceEvaluation e) {
        return ComplianceEvaluationResponseDTO.builder()
                .id(e.getId()).frameworkId(e.getFramework() != null ? e.getFramework().getId() : null).title(e.getTitle())
                .description(e.getDescription()).scope(e.getScope()).status(e.getStatus())
                .evaluatorId(e.getEvaluator() != null ? e.getEvaluator().getId() : null).overallScore(e.getOverallScore())
                .startDate(e.getStartDate()).endDate(e.getEndDate()).ownerId(e.getOwner() != null ? e.getOwner().getId() : null)
                .archived(Boolean.TRUE.equals(e.getArchived()))
                .createdBy(UserSummaryDTO.of(e.getOwner())).createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .build();
    }

    public static ComplianceEvaluation toEntity(ComplianceEvaluationRequestDTO r) {
        return ComplianceEvaluation.builder()
                .title(r.getTitle()).description(r.getDescription())
                .scope(r.getScope())
                .startDate(r.getStartDate()).endDate(r.getEndDate())
                .build();
    }

    public static void updateEntity(ComplianceEvaluation e, ComplianceEvaluationRequestDTO r) {
        if (r.getTitle() != null)       e.setTitle(r.getTitle());
        if (r.getDescription() != null) e.setDescription(r.getDescription());
        if (r.getScope() != null)       e.setScope(r.getScope());
        if (r.getStartDate() != null)   e.setStartDate(r.getStartDate());
        if (r.getEndDate() != null)     e.setEndDate(r.getEndDate());
    }
}