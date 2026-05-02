package com.project.grcplatform.dto;

import com.project.grcplatform.constant.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder
public class RiskRegisterEntryDTO {

    // ── Scénario ──────────────────────────────────────────────────────────────
    private String scenarioId;
    private String scenarioName;
    private String scenarioDescription;
    private ScenarioStatus scenarioStatus;

    // Actif lié
    private String assetId;
    private String assetTitle;
    private Integer assetCriticalityScore;

    // Vulnérabilité liée
    private String vulnerabilityId;
    private String vulnerabilityTitle;
    private VulnCriticality vulnerabilityCriticality;

    // Menace liée
    private String threatId;
    private String threatName;
    private ThreatSeverity threatSeverity;

    // Scores bruts
    private Integer likelihood;
    private Integer impact;
    private Integer rawRiskScore;
    private String rawRiskLevel; // LOW / MEDIUM / HIGH / CRITICAL

    // Scores résiduels
    private Integer controlEvaluation;
    private Integer residualRiskScore;
    private String residualRiskLevel;

    // ── Appréciation liée ────────────────────────────────────────────────────
    private String assessmentId;
    private String assessmentTitle;
    private AssessmentStatus assessmentStatus;
    private LocalDate assessmentEndDate;

    // ── Plan de traitement lié ───────────────────────────────────────────────
    private String treatmentPlanId;
    private String treatmentPlanTitle;
    private TreatmentStrategy treatmentStrategy;
    private TreatmentPlanStatus treatmentPlanStatus;
    private Short treatmentProgressPct;
    private LocalDate treatmentDueDate;
    private UserSummaryDTO treatmentOwner;

    // ── Statut global du risque (calculé) ────────────────────────────────────
    private String riskGlobalStatus; // OPEN / BEING_TREATED / TREATED / ACCEPTED

    // ── Métadonnées ──────────────────────────────────────────────────────────
    private UserSummaryDTO owner;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}