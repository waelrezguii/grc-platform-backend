package com.project.grcplatform.model;

import com.project.grcplatform.base.SoftDeleteEntity;
import com.project.grcplatform.constant.KriLinkedEntityType;
import com.project.grcplatform.constant.KriStatus;
import com.project.grcplatform.constant.ThresholdDirection;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "kris", indexes = {
        @Index(name = "idx_kri_status",        columnList = "status"),
        @Index(name = "idx_kri_linked_entity", columnList = "linked_entity_type, linked_entity_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Kri extends SoftDeleteEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private KriCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "linked_entity_type")
    private KriLinkedEntityType linkedEntityType;

    @Column(name = "linked_entity_id")
    private String linkedEntityId;

    /** Unit of measure — e.g. "%", "count", "days" */
    private String unit;

    @Column(name = "current_value")
    private Double currentValue;

    @Column(name = "warning_threshold", nullable = false)
    private Double warningThreshold;

    @Column(name = "breach_threshold", nullable = false)
    private Double breachThreshold;

    @Enumerated(EnumType.STRING)
    @Column(name = "threshold_direction", nullable = false)
    @Builder.Default
    private ThresholdDirection thresholdDirection = ThresholdDirection.ABOVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private KriStatus status = KriStatus.NORMAL;

    @Column(name = "last_evaluated_at")
    private LocalDateTime lastEvaluatedAt;

    @PrePersist
    public void onCreate() {
        recalculateStatus();
    }

    @PreUpdate
    public void onUpdate() {
        recalculateStatus();
    }
    private void recalculateStatus() {
        if (currentValue == null) {
            this.status = KriStatus.NORMAL;
            return;
        }
        if (thresholdDirection == ThresholdDirection.ABOVE) {
            if (currentValue > breachThreshold)  this.status = KriStatus.BREACH;
            else if (currentValue > warningThreshold) this.status = KriStatus.WARNING;
            else this.status = KriStatus.NORMAL;
        } else {
            if (currentValue < breachThreshold)  this.status = KriStatus.BREACH;
            else if (currentValue < warningThreshold) this.status = KriStatus.WARNING;
            else this.status = KriStatus.NORMAL;
        }
    }
}
