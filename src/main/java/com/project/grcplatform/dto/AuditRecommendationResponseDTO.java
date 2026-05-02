package com.project.grcplatform.dto;

import com.project.grcplatform.constant.RecommendationPriority;
import com.project.grcplatform.constant.RecommendationStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder
public class AuditRecommendationResponseDTO {
    private String id;
    private String campaignId;
    private String campaignTitle;
    private String findingId;
    private String findingTitle;
    private String title;
    private String description;
    private RecommendationPriority priority;
    private RecommendationStatus status;
    private UserSummaryDTO assignee;
    private LocalDate dueDate;
    private LocalDateTime implementedAt;
    private UserSummaryDTO owner;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}