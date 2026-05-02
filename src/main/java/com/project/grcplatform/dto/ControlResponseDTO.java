package com.project.grcplatform.dto;

import com.project.grcplatform.constant.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ControlResponseDTO {

    private String id;
    private String isoReference;
    private String title;
    private String description;
    private ControlCategoryDTO category;
    private ControlTypeDTO type;
    private ControlStatus status;
    private ControlEffectiveness effectiveness;

    // ── French GRC evaluation fields ─────────────────────────────────────────
    private ControlFormalism formalism;
    private ControlNature nature;
    private ControlTiming timing;
    private Boolean tracabilite;
    private Boolean conformite4Yeux;
    private ControlPerformance performance;
    private ControlEfficacy efficacy;  // computed server-side
    // ─────────────────────────────────────────────────────────────────────────

    private UserSummaryDTO owner;
    private String scope;
    private String assetId;
    /** Scenarios mitigated by this control. */
    private List<ScenarioSummaryDTO> scenarios;
    private String validatedBy;
    private LocalDateTime validatedAt;
    private LocalDate lastReviewDate;
    private LocalDate nextReviewDate;
    private String implementationNotes;
    private String evidenceUrl;
    private UserSummaryDTO createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
