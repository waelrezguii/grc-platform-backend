package com.project.grcplatform.dto;

import com.project.grcplatform.constant.PriorityLevel;
import com.project.grcplatform.constant.ScenarioStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RiskScenarioResponseDTO {

    private String id;
    private String name;
    private String description;

    private String assetId;
    private String threatId;
    private String vulnerabilityId;

    private Short likelihood;
    private Short impact;
    private Short rawRiskScore;

    private Short controlEvaluation;
    private Short residualRiskScore;

    private String treatmentPlan;
    private String impactJustification;
    private String lessonsLearned;
    private ScenarioStatus status;

    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String validatedBy;
    private LocalDateTime validatedAt;
    private String lastReviewedBy;
    private LocalDateTime lastReviewedAt;
    private LocalDateTime nextReviewDue;
    private String reviewNotes;

    private Short regulatoryExposure;
    private Double priorityScore;
    private PriorityLevel priorityLevel;

    private boolean archived;
    private UserSummaryDTO createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}