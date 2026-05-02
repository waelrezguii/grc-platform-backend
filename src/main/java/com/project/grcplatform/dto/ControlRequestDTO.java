package com.project.grcplatform.dto;

import com.project.grcplatform.constant.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ControlRequestDTO {

    private String isoReference;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private String categoryId;

    private String typeId;

    private ControlEffectiveness effectiveness;

    // ── French GRC evaluation fields ─────────────────────────────────────────
    private ControlFormalism formalism;
    private ControlNature nature;
    private ControlTiming timing;
    private Boolean tracabilite;
    private Boolean conformite4Yeux;
    private ControlPerformance performance;
    // efficacy is computed server-side — not accepted from client
    // ─────────────────────────────────────────────────────────────────────────

    private String ownerId;

    private String scope;

    private String assetId;

    /** IDs of risk scenarios mitigated by this control (replaces single scenarioId). */
    private List<String> scenarioIds;

    private LocalDate nextReviewDate;

    private String implementationNotes;

    private String evidenceUrl;
}
