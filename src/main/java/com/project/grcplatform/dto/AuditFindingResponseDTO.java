package com.project.grcplatform.dto;

import com.project.grcplatform.constant.FindingSeverity;
import com.project.grcplatform.constant.FindingStatus;
import com.project.grcplatform.constant.FindingType;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class AuditFindingResponseDTO {
    private String id;
    private String campaignId;
    private String campaignTitle;
    private String title;
    private String description;
    private FindingType findingType;
    private FindingSeverity severity;
    private String category;
    private String evidence;
    private String recommendation;
    private FindingStatus status;
    private UserSummaryDTO createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}