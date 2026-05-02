package com.project.grcplatform.mapper;

import com.project.grcplatform.dto.AuditCampaignRequestDTO;
import com.project.grcplatform.dto.AuditCampaignResponseDTO;
import com.project.grcplatform.dto.AuditTypeDTO;
import com.project.grcplatform.dto.UserSummaryDTO;
import com.project.grcplatform.model.AuditCampaign;
import com.project.grcplatform.model.AuditType;
import com.project.grcplatform.model.User;

public class AuditCampaignMapper {
    private AuditCampaignMapper() {}

    public static AuditCampaignResponseDTO toDTO(AuditCampaign c) {
        return AuditCampaignResponseDTO.builder()
                .id(c.getId())
                .title(c.getTitle())
                .description(c.getDescription())
                .auditType(c.getAuditType() != null ? AuditTypeDTO.builder()
                        .id(c.getAuditType().getId())
                        .name(c.getAuditType().getName())
                        .description(c.getAuditType().getDescription())
                        .build() : null)
                .scope(c.getScope())
                .status(c.getStatus())
                .auditor(UserSummaryDTO.of(c.getAuditor()))
                .auditee(UserSummaryDTO.of(c.getAuditee()))
                .plannedStartDate(c.getPlannedStartDate())
                .plannedEndDate(c.getPlannedEndDate())
                .actualStartDate(c.getActualStartDate())
                .actualEndDate(c.getActualEndDate())
                .archived(Boolean.TRUE.equals(c.getArchived()))
                .createdBy(UserSummaryDTO.of(c.getCreatedBy()))
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    public static AuditCampaign toEntity(AuditCampaignRequestDTO r, User auditor,
                                          User auditee, AuditType auditType) {
        return AuditCampaign.builder()
                .title(r.getTitle())
                .description(r.getDescription())
                .auditType(auditType)
                .scope(r.getScope())
                .auditor(auditor)
                .auditee(auditee)
                .plannedStartDate(r.getPlannedStartDate())
                .plannedEndDate(r.getPlannedEndDate())
                .build();
    }

    public static void updateEntity(AuditCampaign c, AuditCampaignRequestDTO r,
                                    User auditor, User auditee, AuditType auditType) {
        if (r.getTitle() != null)            c.setTitle(r.getTitle());
        if (r.getDescription() != null)      c.setDescription(r.getDescription());
        if (auditType != null)               c.setAuditType(auditType);
        if (r.getScope() != null)            c.setScope(r.getScope());
        if (auditor != null)                 c.setAuditor(auditor);
        if (auditee != null)                 c.setAuditee(auditee);
        if (r.getPlannedStartDate() != null) c.setPlannedStartDate(r.getPlannedStartDate());
        if (r.getPlannedEndDate() != null)   c.setPlannedEndDate(r.getPlannedEndDate());
    }

}