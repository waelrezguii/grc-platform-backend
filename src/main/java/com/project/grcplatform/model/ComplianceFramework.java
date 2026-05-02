package com.project.grcplatform.model;

import com.project.grcplatform.base.SoftDeleteEntity;
import com.project.grcplatform.constant.FrameworkStatus;
import com.project.grcplatform.constant.FrameworkType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "compliance_frameworks", indexes = {
        @Index(name = "idx_cf_status", columnList = "status"),
        @Index(name = "idx_cf_type",   columnList = "framework_type"),
        @Index(name = "idx_cf_owner",  columnList = "owner_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ComplianceFramework extends SoftDeleteEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "framework_type", nullable = false)
    private FrameworkType frameworkType;

    private String version;

    private String issuer;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private FrameworkStatus status = FrameworkStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
}
