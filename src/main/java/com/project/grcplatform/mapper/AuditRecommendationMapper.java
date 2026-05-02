package com.project.grcplatform.mapper;

import com.project.grcplatform.dto.AuditRecommendationRequestDTO;
import com.project.grcplatform.dto.AuditRecommendationResponseDTO;
import com.project.grcplatform.dto.UserSummaryDTO;
import com.project.grcplatform.model.AuditCampaign;
import com.project.grcplatform.model.AuditFinding;
import com.project.grcplatform.model.AuditRecommendation;
import com.project.grcplatform.model.User;

public class AuditRecommendationMapper {
    private AuditRecommendationMapper() {}

    public static AuditRecommendationResponseDTO toDTO(AuditRecommendation r) {
        return AuditRecommendationResponseDTO.builder()
                .id(r.getId())
                .campaignId(r.getCampaign() != null ? r.getCampaign().getId() : null)
                .campaignTitle(r.getCampaign() != null ? r.getCampaign().getTitle() : null)
                .findingId(r.getFinding() != null ? r.getFinding().getId() : null)
                .findingTitle(r.getFinding() != null ? r.getFinding().getTitle() : null)
                .title(r.getTitle())
                .description(r.getDescription())
                .priority(r.getPriority())
                .status(r.getStatus())
                .assignee(UserSummaryDTO.of(r.getAssignee()))
                .dueDate(r.getDueDate())
                .implementedAt(r.getImplementedAt())
                .owner(UserSummaryDTO.of(r.getOwner()))
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    public static AuditRecommendation toEntity(AuditRecommendationRequestDTO req,
                                               AuditCampaign campaign,
                                               AuditFinding finding,
                                               User assignee,
                                               User owner) {
        return AuditRecommendation.builder()
                .campaign(campaign)
                .finding(finding)
                .title(req.getTitle())
                .description(req.getDescription())
                .priority(req.getPriority())
                .assignee(assignee)
                .dueDate(req.getDueDate())
                .owner(owner)
                .build();
    }

    public static void updateEntity(AuditRecommendation r, AuditRecommendationRequestDTO req,
                                    AuditFinding finding, User assignee) {
        if (req.getTitle() != null)       r.setTitle(req.getTitle());
        if (req.getDescription() != null) r.setDescription(req.getDescription());
        if (req.getPriority() != null)    r.setPriority(req.getPriority());
        if (req.getDueDate() != null)     r.setDueDate(req.getDueDate());
        if (finding != null)              r.setFinding(finding);
        if (assignee != null)             r.setAssignee(assignee);
    }

}