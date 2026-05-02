package com.project.grcplatform.dto;

import com.project.grcplatform.constant.AuditCampaignStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder
public class AuditCampaignResponseDTO {
    private String id;
    private String title;
    private String description;
    private AuditTypeDTO auditType;
    private String scope;
    private AuditCampaignStatus status;
    private UserSummaryDTO auditor;
    private UserSummaryDTO auditee;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;
    private boolean archived;
    private UserSummaryDTO createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}