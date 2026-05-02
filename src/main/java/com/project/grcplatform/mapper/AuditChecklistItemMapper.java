package com.project.grcplatform.mapper;

import com.project.grcplatform.constant.ChecklistResult;
import com.project.grcplatform.dto.AuditChecklistItemRequestDTO;
import com.project.grcplatform.dto.AuditChecklistItemResponseDTO;
import com.project.grcplatform.model.AuditCampaign;
import com.project.grcplatform.model.AuditChecklistItem;

public class AuditChecklistItemMapper {
    private AuditChecklistItemMapper() {}

    public static AuditChecklistItemResponseDTO toDTO(AuditChecklistItem i) {
        return AuditChecklistItemResponseDTO.builder()
                .id(i.getId())
                .campaignId(i.getCampaign() != null ? i.getCampaign().getId() : null)
                .campaignTitle(i.getCampaign() != null ? i.getCampaign().getTitle() : null)
                .category(i.getCategory())
                .question(i.getQuestion())
                .expectedEvidence(i.getExpectedEvidence())
                .result(i.getResult())
                .actualEvidence(i.getActualEvidence())
                .comment(i.getComment())
                .createdAt(i.getCreatedAt())
                .updatedAt(i.getUpdatedAt())
                .build();
    }

    public static AuditChecklistItem toEntity(AuditChecklistItemRequestDTO r, AuditCampaign campaign) {
        return AuditChecklistItem.builder()
                .campaign(campaign)
                .category(r.getCategory())
                .question(r.getQuestion())
                .expectedEvidence(r.getExpectedEvidence())
                .result(r.getResult() != null ? r.getResult() : ChecklistResult.NOT_EVALUATED)
                .actualEvidence(r.getActualEvidence())
                .comment(r.getComment())
                .build();
    }

    public static void updateEntity(AuditChecklistItem i, AuditChecklistItemRequestDTO r) {
        if (r.getCategory() != null)         i.setCategory(r.getCategory());
        if (r.getQuestion() != null)         i.setQuestion(r.getQuestion());
        if (r.getExpectedEvidence() != null) i.setExpectedEvidence(r.getExpectedEvidence());
        if (r.getResult() != null)           i.setResult(r.getResult());
        if (r.getActualEvidence() != null)   i.setActualEvidence(r.getActualEvidence());
        if (r.getComment() != null)          i.setComment(r.getComment());
    }
}