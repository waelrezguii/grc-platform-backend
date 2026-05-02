package com.project.grcplatform.model;

import com.project.grcplatform.base.SoftDeleteEntity;
import com.project.grcplatform.constant.ResponsibilityStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "governance_responsibilities", indexes = {
        @Index(name = "idx_gresp_assignee", columnList = "assignee_id"),
        @Index(name = "idx_gresp_status",   columnList = "status"),
        @Index(name = "idx_gresp_type",     columnList = "responsibility_type_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GovernanceResponsibility extends SoftDeleteEntity {

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsibility_type_id")
    private ResponsibilityType responsibilityType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id", nullable = false)
    private User assignee;

    @Column(columnDefinition = "TEXT")
    private String scope;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ResponsibilityStatus status = ResponsibilityStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
}
