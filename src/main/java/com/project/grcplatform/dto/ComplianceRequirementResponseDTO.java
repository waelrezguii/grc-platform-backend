package com.project.grcplatform.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class ComplianceRequirementResponseDTO {
    private String id;
    private String frameworkId;
    private String code;
    private String title;
    private String description;
    private String category;
    private Boolean mandatory;
    private UserSummaryDTO createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}