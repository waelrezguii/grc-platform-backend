package com.project.grcplatform.model;

import com.project.grcplatform.base.BaseEntity;
import com.project.grcplatform.constant.TreatmentStrategy;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "risk_assessment_scenarios",
        uniqueConstraints = @UniqueConstraint(columnNames = {"assessment_id", "scenario_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RiskAssessmentScenario extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", nullable = false)
    private RiskAssessment assessment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private RiskScenario scenario;

    @Enumerated(EnumType.STRING)
    @Column(name = "treatment_strategy")
    private TreatmentStrategy treatmentStrategy;

    @Column(name = "treatment_notes", columnDefinition = "TEXT")
    private String treatmentNotes;

    @Column(name = "added_by")
    private String addedBy;

    @Column(name = "added_at", nullable = false)
    @Builder.Default
    private LocalDateTime addedAt = LocalDateTime.now();
}
