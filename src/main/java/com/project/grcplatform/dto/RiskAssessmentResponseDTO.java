package com.project.grcplatform.dto;

import com.project.grcplatform.constant.AssessmentStatus;
import com.project.grcplatform.constant.TreatmentStrategy;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class RiskAssessmentResponseDTO {

    private String id;
    private String title;
    private String description;
    private String organisationId;
    private LocalDate startDate;
    private LocalDate endDate;
    private AssessmentStatus status;
    private Short overallRiskScore;
    private Short overallResidualScore;
    private TreatmentStrategy treatmentStrategy;
    private String treatmentNotes;
    private String riskOwnerId;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private UserSummaryDTO createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}