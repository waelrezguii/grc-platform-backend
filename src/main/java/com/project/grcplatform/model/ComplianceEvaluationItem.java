package com.project.grcplatform.model;

import com.project.grcplatform.base.SoftDeleteEntity;
import com.project.grcplatform.constant.ComplianceLevel;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "compliance_evaluation_items", indexes = {
        @Index(name = "idx_citem_evaluation",  columnList = "evaluation_id"),
        @Index(name = "idx_citem_requirement", columnList = "requirement_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ComplianceEvaluationItem extends SoftDeleteEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private ComplianceEvaluation evaluation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requirement_id", nullable = false)
    private ComplianceRequirement requirement;

    @Enumerated(EnumType.STRING)
    @Column(name = "compliance_level", nullable = false)
    @Builder.Default
    private ComplianceLevel complianceLevel = ComplianceLevel.NON_COMPLIANT;
    private Double score;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(columnDefinition = "TEXT")
    private String evidence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @PrePersist
    public void onCreate() {
        deriveScore();
    }

    @PreUpdate
    public void onUpdate() {
        deriveScore();
    }


    //! REMOVE HARD CODED VALUE
    public void deriveScore() {
        if (complianceLevel == null) return;
        this.score = complianceLevel.score;
    }
}
