package com.project.grcplatform.dto;

import com.project.grcplatform.constant.EvaluationStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder
public class ComplianceEvaluationResponseDTO {
    private String id;
    private String frameworkId;
    private String title;
    private String description;
    private String scope;
    private EvaluationStatus status;
    private String evaluatorId;
    private Double overallScore;
    private LocalDate startDate;
    private LocalDate endDate;
    private String ownerId;
    private boolean archived;
    private UserSummaryDTO createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}