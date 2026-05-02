package com.project.grcplatform.model;

import com.project.grcplatform.base.SoftDeleteEntity;
import com.project.grcplatform.constant.AuditCampaignStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "audit_campaigns", indexes = {
        @Index(name = "idx_ac_status",  columnList = "status"),
        @Index(name = "idx_ac_auditor", columnList = "auditor_id"),
        @Index(name = "idx_ac_auditee", columnList = "auditee_id"),
        @Index(name = "idx_ac_type",    columnList = "audit_type_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditCampaign extends SoftDeleteEntity {

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_type_id")
    private AuditType auditType;

    @Column(columnDefinition = "TEXT")
    private String scope;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AuditCampaignStatus status = AuditCampaignStatus.PLANNED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auditor_id")
    private User auditor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auditee_id")
    private User auditee;

    @Column(name = "planned_start_date")
    private LocalDate plannedStartDate;

    @Column(name = "planned_end_date")
    private LocalDate plannedEndDate;

    @Column(name = "actual_start_date")
    private LocalDate actualStartDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean archived = false;
}
