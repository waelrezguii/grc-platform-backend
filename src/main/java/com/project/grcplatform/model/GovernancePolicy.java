package com.project.grcplatform.model;

import com.project.grcplatform.base.SoftDeleteEntity;
import com.project.grcplatform.constant.PolicyStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "governance_policies", indexes = {
        @Index(name = "idx_gpolicy_status",  columnList = "status"),
        @Index(name = "idx_gpolicy_type",    columnList = "policy_type_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GovernancePolicy extends SoftDeleteEntity {

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_type_id")
    private PolicyType policyType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PolicyStatus status = PolicyStatus.DRAFT;

    private String version;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

}
