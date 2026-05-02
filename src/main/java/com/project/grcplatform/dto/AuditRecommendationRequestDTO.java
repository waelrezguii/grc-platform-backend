package com.project.grcplatform.dto;

import com.project.grcplatform.constant.RecommendationPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class AuditRecommendationRequestDTO {
    @NotBlank private String campaignId;  // FK → AuditCampaign
    private String findingId;
    @NotBlank private String title;
    private String description;
    @NotNull  private RecommendationPriority priority;
    private String assigneeId;            // FK → User
    private LocalDate dueDate;
}