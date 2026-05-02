package com.project.grcplatform.mapper;

import com.project.grcplatform.dto.ComplianceEvaluationItemRequestDTO;
import com.project.grcplatform.dto.ComplianceEvaluationItemResponseDTO;
import com.project.grcplatform.dto.UserSummaryDTO;
import com.project.grcplatform.model.ComplianceEvaluationItem;

public class ComplianceEvaluationItemMapper {
    private ComplianceEvaluationItemMapper() {}

    public static ComplianceEvaluationItemResponseDTO toDTO(ComplianceEvaluationItem i) {
        return ComplianceEvaluationItemResponseDTO.builder()
                .id(i.getId()).evaluationId(i.getEvaluation() != null ? i.getEvaluation().getId() : null).requirementId(i.getRequirement() != null ? i.getRequirement().getId() : null)
                .complianceLevel(i.getComplianceLevel()).score(i.getScore())
                .comment(i.getComment()).evidence(i.getEvidence())
                .createdBy(UserSummaryDTO.of(i.getOwner())).createdAt(i.getCreatedAt()).updatedAt(i.getUpdatedAt())
                .build();
    }

    public static ComplianceEvaluationItem toEntity(ComplianceEvaluationItemRequestDTO r, String evaluationId) {
        return ComplianceEvaluationItem.builder()
                .complianceLevel(r.getComplianceLevel())
                .comment(r.getComment()).evidence(r.getEvidence())
                .build();
    }

    public static void updateEntity(ComplianceEvaluationItem i, ComplianceEvaluationItemRequestDTO r) {
        if (r.getComplianceLevel() != null) i.setComplianceLevel(r.getComplianceLevel());
        if (r.getComment() != null)         i.setComment(r.getComment());
        if (r.getEvidence() != null)        i.setEvidence(r.getEvidence());
        // score is re-derived in @PreUpdate
    }
}