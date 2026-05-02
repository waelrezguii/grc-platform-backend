package com.project.grcplatform.model;

import com.project.grcplatform.base.SoftDeleteEntity;
import com.project.grcplatform.constant.AssessmentStatus;
import com.project.grcplatform.constant.TreatmentStrategy;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "risk_assessments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RiskAssessment extends SoftDeleteEntity {

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id", nullable = false)
    private Organisation organisation;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AssessmentStatus status = AssessmentStatus.PLANNED;

    @Column(name = "overall_risk_score")
    private Short overallRiskScore;

    @Column(name = "overall_residual_score")
    private Short overallResidualScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "treatment_strategy")
    private TreatmentStrategy treatmentStrategy;

    @Column(name = "treatment_notes", columnDefinition = "TEXT")
    private String treatmentNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "risk_owner_id")
    private User riskOwner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
}
