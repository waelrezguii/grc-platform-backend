package com.project.grcplatform.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data @Builder
public class RiskRegisterSummaryDTO {

    // Totaux
    private long totalRisks;
    private long openRisks;
    private long beingTreatedRisks;
    private long treatedRisks;
    private long acceptedRisks;

    // Par niveau de risque brut
    private Map<String, Long> byRawRiskLevel;      // {LOW: 5, MEDIUM: 10, HIGH: 8, CRITICAL: 3}

    // Par niveau de risque résiduel
    private Map<String, Long> byResidualRiskLevel;

    // Par stratégie de traitement
    private Map<String, Long> byTreatmentStrategy; // {REDUCE: 12, ACCEPT: 4, TRANSFER: 2, AVOID: 1}

    // Par statut de scénario
    private Map<String, Long> byScenarioStatus;

    // Top 5 actifs les plus risqués (par rawRiskScore max)
    private java.util.List<String> topRiskyAssets;

    // Scores moyens
    private Double averageRawRiskScore;
    private Double averageResidualRiskScore;
    private Double riskReductionRate; // % de réduction (brut → résiduel)
}