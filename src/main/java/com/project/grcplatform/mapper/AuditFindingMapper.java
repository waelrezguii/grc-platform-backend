package com.project.grcplatform.mapper;

import com.project.grcplatform.dto.AuditFindingRequestDTO;
import com.project.grcplatform.dto.AuditFindingResponseDTO;
import com.project.grcplatform.dto.UserSummaryDTO;
import com.project.grcplatform.model.AuditCampaign;
import com.project.grcplatform.model.AuditFinding;

public class AuditFindingMapper {
    private AuditFindingMapper() {}

    public static AuditFindingResponseDTO toDTO(AuditFinding f) {
        return AuditFindingResponseDTO.builder()
                .id(f.getId())
                .campaignId(f.getCampaign() != null ? f.getCampaign().getId() : null)
                .campaignTitle(f.getCampaign() != null ? f.getCampaign().getTitle() : null)
                .title(f.getTitle())
                .description(f.getDescription())
                .findingType(f.getFindingType())
                .severity(f.getSeverity())
                .category(f.getCategory())
                .evidence(f.getEvidence())
                .recommendation(f.getRecommendation())
                .status(f.getStatus())
                .createdBy(UserSummaryDTO.of(f.getCreatedBy()))
                .createdAt(f.getCreatedAt())
                .updatedAt(f.getUpdatedAt())
                .build();
    }

    public static AuditFinding toEntity(AuditFindingRequestDTO r, AuditCampaign campaign) {
        return AuditFinding.builder()
                .campaign(campaign)
                .title(r.getTitle())
                .description(r.getDescription())
                .findingType(r.getFindingType())
                .severity(r.getSeverity())
                .category(r.getCategory())
                .evidence(r.getEvidence())
                .recommendation(r.getRecommendation())
                .build();
    }

    public static void updateEntity(AuditFinding f, AuditFindingRequestDTO r) {
        if (r.getTitle() != null)          f.setTitle(r.getTitle());
        if (r.getDescription() != null)    f.setDescription(r.getDescription());
        if (r.getFindingType() != null)    f.setFindingType(r.getFindingType());
        if (r.getSeverity() != null)       f.setSeverity(r.getSeverity());
        if (r.getCategory() != null)       f.setCategory(r.getCategory());
        if (r.getEvidence() != null)       f.setEvidence(r.getEvidence());
        if (r.getRecommendation() != null) f.setRecommendation(r.getRecommendation());
    }

}