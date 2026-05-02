package com.project.grcplatform.model;

import com.project.grcplatform.base.SoftDeleteEntity;
import com.project.grcplatform.constant.RecommendationPriority;
import com.project.grcplatform.constant.RecommendationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_recommendations", indexes = {
        @Index(name = "idx_ar_campaign", columnList = "campaign_id"),
        @Index(name = "idx_ar_finding",  columnList = "finding_id"),
        @Index(name = "idx_ar_assignee", columnList = "assignee_id"),
        @Index(name = "idx_ar_owner",    columnList = "owner_id"),
        @Index(name = "idx_ar_status",   columnList = "status"),
        @Index(name = "idx_ar_due_date", columnList = "due_date")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditRecommendation extends SoftDeleteEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private AuditCampaign campaign;

    // Optionnel — lié à un constat spécifique
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finding_id")
    private AuditFinding finding;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RecommendationPriority priority = RecommendationPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RecommendationStatus status = RecommendationStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "implemented_at")
    private LocalDateTime implementedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
}
