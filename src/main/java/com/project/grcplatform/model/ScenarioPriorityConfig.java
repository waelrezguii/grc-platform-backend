package com.project.grcplatform.model;

import com.project.grcplatform.base.SoftDeleteEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "scenario_priority_configs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScenarioPriorityConfig extends SoftDeleteEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Weight for likelihood criterion (0.0 – 1.0). */
    @Column(name = "likelihood_weight", nullable = false)
    @Builder.Default
    private Double likelihoodWeight = 0.3;

    /** Weight for impact criterion (0.0 – 1.0). */
    @Column(name = "impact_weight", nullable = false)
    @Builder.Default
    private Double impactWeight = 0.3;

    /** Weight for regulatory exposure criterion (0.0 – 1.0). */
    @Column(name = "regulatory_exposure_weight", nullable = false)
    @Builder.Default
    private Double regulatoryExposureWeight = 0.2;

    /** Weight for CVSS score from linked vulnerability (0.0 – 1.0). */
    @Column(name = "cvss_weight", nullable = false)
    @Builder.Default
    private Double cvssWeight = 0.2;

    /**
     * Priority score threshold (0–100) at which a SCENARIO_PRIORITY_ALERT
     * notification is sent to the scenario owner.
     */
    @Column(name = "alert_threshold", nullable = false)
    @Builder.Default
    private Double alertThreshold = 75.0;

    /**
     * Only one config can be active at a time.
     * When a config is activated all others are deactivated.
     */
    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean active = false;
}
