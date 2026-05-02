package com.project.grcplatform.model;

import com.project.grcplatform.base.BaseEntity;
import com.project.grcplatform.constant.IncidentSeverity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A real incident that materialised from a risk scenario.
 * Linked to a scenario to enrich the history timeline and annual review.
 */
@Entity
@Table(name = "scenario_incidents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScenarioIncident extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private RiskScenario scenario;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentSeverity severity;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /**
     * Documented real-world impact observed after the incident.
     * Used to calibrate the scenario's impact score (REX).
     */
    @Column(name = "actual_impact", columnDefinition = "TEXT")
    private String actualImpact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by")
    private User reportedBy;
}
