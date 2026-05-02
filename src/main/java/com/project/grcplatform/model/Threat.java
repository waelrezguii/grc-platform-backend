package com.project.grcplatform.model;

import com.project.grcplatform.base.SoftDeleteEntity;
import com.project.grcplatform.constant.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "threats")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Threat extends SoftDeleteEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Motivation or intent behind this threat (e.g. financial gain, espionage, sabotage). */
    @Column(columnDefinition = "TEXT")
    private String motivation;

    /** Goals the threat actor aims to achieve (e.g. data exfiltration, service disruption, ransom). */
    @Column(columnDefinition = "TEXT")
    private String objectives;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ThreatOrigin origin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "threat_type_id")
    private ThreatType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ThreatFrequency frequency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ThreatSeverity severity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(columnDefinition = "TEXT")
    private String scenario;

    @Column(columnDefinition = "TEXT")
    private String vectors;

    @Column(name = "aggravating_factors", columnDefinition = "TEXT")
    private String aggravatingFactors;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ThreatStatus status = ThreatStatus.DRAFT;

    @Column(name = "validated_by")
    private String validatedBy;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    // ── Periodic review tracking ──────────────────────────────────────────────

    /** User ID of the last reviewer. */
    @Column(name = "last_reviewed_by")
    private String lastReviewedBy;

    /** Timestamp of the last periodic review. */
    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    /** Due date for the next scheduled review. */
    @Column(name = "next_review_due")
    private LocalDateTime nextReviewDue;

    /** Notes written by the reviewer during the last review. */
    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    /** Assets directly exposed to this threat. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "threat_assets",
            joinColumns = @JoinColumn(name = "threat_id"),
            inverseJoinColumns = @JoinColumn(name = "asset_id")
    )
    @Builder.Default
    private List<Asset> assets = new ArrayList<>();

    /** Vulnerabilities that this threat can exploit. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "threat_vulnerabilities",
            joinColumns = @JoinColumn(name = "threat_id"),
            inverseJoinColumns = @JoinColumn(name = "vulnerability_id")
    )
    @Builder.Default
    private List<Vulnerability> vulnerabilities = new ArrayList<>();
}
