package com.project.grcplatform.model;

import com.project.grcplatform.base.SoftDeleteEntity;
import com.project.grcplatform.constant.FindingSeverity;
import com.project.grcplatform.constant.FindingStatus;
import com.project.grcplatform.constant.FindingType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "audit_findings", indexes = {
        @Index(name = "idx_af_campaign", columnList = "campaign_id"),
        @Index(name = "idx_af_status",   columnList = "status"),
        @Index(name = "idx_af_severity", columnList = "severity"),
        @Index(name = "idx_af_type",     columnList = "finding_type"),
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditFinding extends SoftDeleteEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private AuditCampaign campaign;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "finding_type", nullable = false)
    private FindingType findingType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FindingSeverity severity;

    private String category;

    @Column(columnDefinition = "TEXT")
    private String evidence;

    @Column(columnDefinition = "TEXT")
    private String recommendation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private FindingStatus status = FindingStatus.OPEN;

}
