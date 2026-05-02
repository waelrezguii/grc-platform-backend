package com.project.grcplatform.model;

import com.project.grcplatform.base.SoftDeleteEntity;
import com.project.grcplatform.constant.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "controls")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Control extends SoftDeleteEntity {

    // ISO 27001 reference e.g. "A.8.1", "A.9.4.2"
    @Column(name = "iso_reference")
    private String isoReference;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "control_category_id")
    private ControlCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "control_type_id")
    private ControlType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ControlStatus status = ControlStatus.PLANNED;

    @Enumerated(EnumType.STRING)
    private ControlEffectiveness effectiveness;

    // ── French GRC control evaluation fields ─────────────────────────────────

    /** Formalisme du contrôle: is the control formally documented? (Oui = FORMAL) */
    @Enumerated(EnumType.STRING)
    @Column(name = "formalism")
    private ControlFormalism formalism;

    /** Type de contrôle: PREVENTIF, DETECTIF, CORRECTIF */
    @Enumerated(EnumType.STRING)
    @Column(name = "nature")
    private ControlNature nature;

    /** Timing du contrôle: AUTOMATIQUE, SEMI_AUTOMATIQUE, MANUEL */
    @Enumerated(EnumType.STRING)
    @Column(name = "timing")
    private ControlTiming timing;

    /** Traçabilité du contrôle: is the control traceable? */
    @Column(name = "tracabilite")
    private Boolean tracabilite;

    /** Conformité au principe des 4 yeux (compatibilité des tâches): true = Oui */
    @Column(name = "conformite_4_yeux")
    private Boolean conformite4Yeux;

    /** Performance du contrôle: PERFORMANT, MOYENNEMENT_PERFORMANT, NON_PERFORMANT */
    @Enumerated(EnumType.STRING)
    @Column(name = "performance")
    private ControlPerformance performance;

    /**
     * Efficacité du contrôle — computed automatically; never set by the client.
     * Recalculated every time the control is saved via {@link #computeEfficacy()}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "efficacy")
    private ControlEfficacy efficacy;

    /**
     * Recomputes {@link #efficacy} from the current field values.
     * Call this before every save when any evaluation field may have changed.
     */
    public void computeEfficacy() {
        if (nature == null || timing == null || performance == null
                || formalism == null || conformite4Yeux == null) {
            efficacy = null;
            return;
        }

        boolean isInexistant =
                (timing == ControlTiming.AUTOMATIQUE && performance == ControlPerformance.NON_PERFORMANT)
                || (nature == ControlNature.CORRECTIF
                        && performance == ControlPerformance.MOYENNEMENT_PERFORMANT
                        && (timing == ControlTiming.SEMI_AUTOMATIQUE || timing == ControlTiming.MANUEL));

        if (isInexistant) {
            efficacy = ControlEfficacy.INEXISTANT;
            return;
        }

        boolean isInsuffisant =
                formalism == ControlFormalism.NON_FORMAL
                || nature == ControlNature.CORRECTIF
                || !conformite4Yeux;

        efficacy = isInsuffisant ? ControlEfficacy.INSUFFISANT : ControlEfficacy.EFFICACE;
    }

    // ─────────────────────────────────────────────────────────────────────────

    /** Responsable du contrôle (security owner). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    /** Portée organisationnelle ou technique du contrôle (free text). */
    @Column(columnDefinition = "TEXT")
    private String scope;

    // Linked to an asset it protects (optional — can be global)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    /**
     * Risk scenarios mitigated by this control.
     * Replaces the previous single-scenario FK to support the spec requirement
     * "un ou plusieurs scénarios de risque".
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "control_scenarios",
            joinColumns = @JoinColumn(name = "control_id"),
            inverseJoinColumns = @JoinColumn(name = "scenario_id")
    )
    @Builder.Default
    private List<RiskScenario> scenarios = new ArrayList<>();

    // Validation by security referent
    @Column(name = "validated_by")
    private String validatedBy;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    // Review cycle
    @Column(name = "last_review_date")
    private LocalDate lastReviewDate;

    @Column(name = "next_review_date")
    private LocalDate nextReviewDate;

    @Column(name = "implementation_notes", columnDefinition = "TEXT")
    private String implementationNotes;

    @Column(name = "evidence_url")
    private String evidenceUrl;
}
