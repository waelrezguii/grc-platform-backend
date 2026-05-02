package com.project.grcplatform.model;

import com.project.grcplatform.base.SoftDeleteEntity;
import com.project.grcplatform.constant.LifecycleStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "assets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Asset extends SoftDeleteEntity {

    @Column(nullable = false, unique = true)
    private String ref;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_category_id")
    private AssetCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_type_id")
    private AssetType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "direction_centrale_id")
    private Organisation directionCentrale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "direction_id")
    private Organisation direction;

    // Nullable — not used for PROCESS assets
    private Short confidentiality;
    private Short integrity;

    @Column(nullable = false)
    private Short availability;

    @Column(name = "criticality_score")
    private Short criticalityScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_status", nullable = false)
    @Builder.Default
    private LifecycleStatus lifecycleStatus = LifecycleStatus.ACQUISITION;

    private LocalDate acquisitionDate;
    private LocalDate endOfLifeDate;
    private LocalDate warrantyExpiryDate;

    /** Network IP address — used for matching scanner results to this asset. */
    @Column(name = "ip_address")
    private String ipAddress;

    /** Hostname / FQDN — used for matching scanner results to this asset. */
    private String hostname;
}
