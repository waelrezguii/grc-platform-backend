package com.project.grcplatform.dto;

import com.project.grcplatform.constant.TreatmentPlanStatus;
import com.project.grcplatform.constant.TreatmentStrategy;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TreatmentPlanResponseDTO {

    private String id;
    private String title;
    private String description;
    private String scenarioId;
    private String assessmentId;
    private TreatmentStrategy treatmentStrategy;
    private TreatmentPlanStatus status;
    private LocalDate dueDate;
    private BigDecimal estimatedBudget;
    private BigDecimal actualCost;
    private Short progressPct;
    private Short treatmentEffectiveness;
    private Short targetRiskScore;
    private boolean archived;
    private UserSummaryDTO createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}