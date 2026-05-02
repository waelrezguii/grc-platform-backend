package com.project.grcplatform.dto;

import com.project.grcplatform.constant.ComplianceLevel;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class ComplianceEvaluationItemResponseDTO {
    private String id;
    private String evaluationId;
    private String requirementId;
    private ComplianceLevel complianceLevel;
    private Double score;
    private String comment;
    private String evidence;
    private UserSummaryDTO createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}