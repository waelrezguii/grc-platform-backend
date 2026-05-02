package com.project.grcplatform.service;

import com.project.grcplatform.constant.*;
import com.project.grcplatform.dto.*;
import com.project.grcplatform.model.*;
import com.project.grcplatform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RiskRegisterService {

    private final RiskScenarioRepository        scenarioRepository;
    private final RiskAssessmentRepository       assessmentRepository;
    private final TreatmentPlanRepository        treatmentPlanRepository;

    // ─── Complete register ───────────────────────────────────────────────────

    public Page<RiskRegisterEntryDTO> getRegister(
            String assetId,
            String threatId,
            String rawRiskLevel,
            String globalStatus,
            TreatmentStrategy treatmentStrategy,
            Pageable pageable) {

        // Fetch all validated (non-deleted) scenarios with optional filters
        Page<RiskScenario> scenarios = scenarioRepository.findForRegister(
                assetId, threatId, rawRiskLevel, globalStatus, treatmentStrategy, pageable);

        List<RiskRegisterEntryDTO> entries = scenarios.getContent().stream()
                .map(this::toRegisterEntry)
                .collect(Collectors.toList());

        return new PageImpl<>(entries, pageable, scenarios.getTotalElements());
    }

    public RiskRegisterEntryDTO getRegisterEntry(String scenarioId) {
        RiskScenario scenario = scenarioRepository.findByIdAndDeletedFalse(scenarioId)
                .orElseThrow(() -> new com.project.grcplatform.exception.AppException(
                        com.project.grcplatform.constant.ErrorCode.SCENARIO_NOT_FOUND));
        return toRegisterEntry(scenario);
    }

    // ─── Résumé statistique ──────────────────────────────────────────────────

    public RiskRegisterSummaryDTO getSummary() {
        List<RiskScenario> allScenarios = scenarioRepository.findAllValidatedForRegister();

        long total         = allScenarios.size();
        long open          = allScenarios.stream().filter(s -> "OPEN".equals(computeGlobalStatus(s))).count();
        long beingTreated  = allScenarios.stream().filter(s -> "BEING_TREATED".equals(computeGlobalStatus(s))).count();
        long treated       = allScenarios.stream().filter(s -> "TREATED".equals(computeGlobalStatus(s))).count();
        long accepted      = allScenarios.stream().filter(s -> "ACCEPTED".equals(computeGlobalStatus(s))).count();

        Map<String, Long> byRawLevel = allScenarios.stream()
                .collect(Collectors.groupingBy(
                        s -> computeRiskLevel(s.getRawRiskScore()),
                        Collectors.counting()));

        Map<String, Long> byResidualLevel = allScenarios.stream()
                .collect(Collectors.groupingBy(
                        s -> computeRiskLevel(s.getResidualRiskScore()),
                        Collectors.counting()));

        Map<String, Long> byStatus = allScenarios.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getStatus() != null ? s.getStatus().name() : "UNKNOWN",
                        Collectors.counting()));

        // Treatment strategy — fetch from treatment plans
        List<TreatmentPlan> plans = treatmentPlanRepository.findAllActiveForRegister();
        Map<String, Long> byStrategy = plans.stream()
                .filter(p -> p.getTreatmentStrategy() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getTreatmentStrategy().name(),
                        Collectors.counting()));

        // Top 5 assets by max rawRiskScore
        List<String> topAssets = allScenarios.stream()
                .filter(s -> s.getAsset() != null)
                .collect(Collectors.groupingBy(
                        s -> s.getAsset().getName(),
                        Collectors.summingInt(s -> s.getRawRiskScore() != null ? s.getRawRiskScore() : 0)))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        double avgRaw = allScenarios.stream()
                .mapToInt(s -> s.getRawRiskScore() != null ? s.getRawRiskScore() : 0)
                .average().orElse(0.0);

        double avgResidual = allScenarios.stream()
                .mapToInt(s -> s.getResidualRiskScore() != null ? s.getResidualRiskScore() : 0)
                .average().orElse(0.0);

        double reductionRate = avgRaw > 0
                ? Math.round(((avgRaw - avgResidual) / avgRaw) * 1000.0) / 10.0
                : 0.0;

        return RiskRegisterSummaryDTO.builder()
                .totalRisks(total)
                .openRisks(open)
                .beingTreatedRisks(beingTreated)
                .treatedRisks(treated)
                .acceptedRisks(accepted)
                .byRawRiskLevel(byRawLevel)
                .byResidualRiskLevel(byResidualLevel)
                .byScenarioStatus(byStatus)
                .byTreatmentStrategy(byStrategy)
                .topRiskyAssets(topAssets)
                .averageRawRiskScore(Math.round(avgRaw * 10.0) / 10.0)
                .averageResidualRiskScore(Math.round(avgResidual * 10.0) / 10.0)
                .riskReductionRate(reductionRate)
                .build();
    }

    // ─── Heatmap ─────────────────────────────────────────────────────────────

    public RiskHeatmapDTO getHeatmap() {
        List<RiskScenario> scenarios = scenarioRepository.findAllValidatedForRegister();

        // Build 5x5 matrix
        Map<String, List<String>> cellMap = new HashMap<>();
        for (int l = 1; l <= 5; l++) {
            for (int i = 1; i <= 5; i++) {
                cellMap.put(l + "_" + i, new ArrayList<>());
            }
        }

        for (RiskScenario s : scenarios) {
            int l = s.getLikelihood() != null ? Math.min(s.getLikelihood(), 5) : 1;
            int i = s.getImpact()     != null ? Math.min(s.getImpact(), 5)     : 1;
            cellMap.get(l + "_" + i).add(s.getId());
        }

        // Inherent risk matrix [fréquence-1][sévérité-1]
        int[][] inherentMatrix = {
            {1, 1, 1, 2, 2},
            {1, 1, 2, 2, 3},
            {1, 2, 2, 3, 4},
            {2, 2, 3, 4, 5},
            {2, 3, 4, 5, 5},
        };

        List<RiskHeatmapDTO.HeatmapCell> cells = new ArrayList<>();
        for (int l = 1; l <= 5; l++) {
            for (int i = 1; i <= 5; i++) {
                List<String> ids = cellMap.get(l + "_" + i);
                int score = inherentMatrix[l - 1][i - 1];
                cells.add(RiskHeatmapDTO.HeatmapCell.builder()
                        .likelihood(l)
                        .impact(i)
                        .riskScore(score)
                        .riskLevel(computeRiskLevel(score))
                        .count(ids.size())
                        .scenarioIds(ids)
                        .build());
            }
        }

        return RiskHeatmapDTO.builder().cells(cells).build();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private RiskRegisterEntryDTO toRegisterEntry(RiskScenario s) {
        // Find linked assessment (most recent completed or in-progress)
        RiskAssessment assessment = assessmentRepository
                .findLatestByScenarioId(s.getId()).orElse(null);

        // Find linked treatment plan (most recent non-cancelled)
        TreatmentPlan plan = treatmentPlanRepository
                .findLatestByScenarioId(s.getId()).stream().findFirst().orElse(null);

        return RiskRegisterEntryDTO.builder()
                // Scenario
                .scenarioId(s.getId())
                .scenarioName(s.getName())
                .scenarioDescription(s.getDescription())
                .scenarioStatus(s.getStatus())
                // Asset
                .assetId(s.getAsset() != null ? s.getAsset().getId() : null)
                .assetTitle(s.getAsset() != null ? s.getAsset().getName() : null)
                .assetCriticalityScore(s.getAsset() != null && s.getAsset().getCriticalityScore() != null ? s.getAsset().getCriticalityScore().intValue() : null)
                // Vulnerability
                .vulnerabilityId(s.getVulnerability() != null ? s.getVulnerability().getId() : null)
                .vulnerabilityTitle(s.getVulnerability() != null ? s.getVulnerability().getTitle() : null)
                .vulnerabilityCriticality(s.getVulnerability() != null ? s.getVulnerability().getCriticality() : null)
                // Threat
                .threatId(s.getThreat() != null ? s.getThreat().getId() : null)
                .threatName(s.getThreat() != null ? s.getThreat().getName() : null)
                .threatSeverity(s.getThreat() != null ? s.getThreat().getSeverity() : null)
                // Raw scores
                .likelihood(s.getLikelihood() != null ? s.getLikelihood().intValue() : null)
                .impact(s.getImpact() != null ? s.getImpact().intValue() : null)
                .rawRiskScore(s.getRawRiskScore() != null ? s.getRawRiskScore().intValue() : null)
                .rawRiskLevel(computeRiskLevel(s.getRawRiskScore()))
                // Residual scores
                .controlEvaluation(s.getControlEvaluation() != null ? s.getControlEvaluation().intValue() : null)
                .residualRiskScore(s.getResidualRiskScore() != null ? s.getResidualRiskScore().intValue() : null)
                .residualRiskLevel(computeRiskLevel(s.getResidualRiskScore()))
                // Assessment
                .assessmentId(assessment != null ? assessment.getId() : null)
                .assessmentTitle(assessment != null ? assessment.getTitle() : null)
                .assessmentStatus(assessment != null ? assessment.getStatus() : null)
                .assessmentEndDate(assessment != null ? assessment.getEndDate() : null)
                // Treatment plan
                .treatmentPlanId(plan != null ? plan.getId() : null)
                .treatmentPlanTitle(plan != null ? plan.getTitle() : null)
                .treatmentStrategy(plan != null ? plan.getTreatmentStrategy() : null)
                .treatmentPlanStatus(plan != null ? plan.getStatus() : null)
                .treatmentProgressPct(plan != null ? plan.getProgressPct() : null)
                .treatmentDueDate(plan != null ? plan.getDueDate() : null)
                .treatmentOwner(plan != null && plan.getCreatedBy() != null
                        ? UserSummaryDTO.builder()
                        .id(plan.getCreatedBy().getId())
                        .firstname(plan.getCreatedBy().getFirstname())
                        .lastname(plan.getCreatedBy().getLastname())
                        .email(plan.getCreatedBy().getEmail())
                        .build()
                        : null)
                // Global status
                .riskGlobalStatus(computeGlobalStatus(s))
                // Owner
                .owner(s.getCreatedBy() != null
                        ? UserSummaryDTO.builder()
                        .id(s.getCreatedBy().getId())
                        .firstname(s.getCreatedBy().getFirstname())
                        .lastname(s.getCreatedBy().getLastname())
                        .email(s.getCreatedBy().getEmail())
                        .build()
                        : null)
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    /**
     * Compute global risk status based on treatment plan state.
     * ACCEPTED  → treatment strategy is ACCEPT
     * TREATED   → treatment plan is COMPLETED
     * BEING_TREATED → treatment plan exists and IN_PROGRESS or APPROVED
     * OPEN      → no treatment plan
     */
    private String computeGlobalStatus(RiskScenario s) {
        TreatmentPlan plan = treatmentPlanRepository
                .findLatestByScenarioId(s.getId()).stream().findFirst().orElse(null);

        if (plan == null) return "OPEN";

        if (plan.getTreatmentStrategy() == TreatmentStrategy.ACCEPT) return "ACCEPTED";

        return switch (plan.getStatus()) {
            case COMPLETED             -> "TREATED";
            case IN_PROGRESS, APPROVED -> "BEING_TREATED";
            default                    -> "OPEN";
        };
    }

    /**
     * Compute risk level label from a matrix score (1–5).
     * 1   → Mineur
     * 2   → Notable
     * 3   → Fort
     * 4   → Très fort
     * 5   → Critique
     */
    private String computeRiskLevel(Number score) {
        if (score == null || score.intValue() <= 0) return "MINEUR";
        return switch (score.intValue()) {
            case 1 -> "MINEUR";
            case 2 -> "NOTABLE";
            case 3 -> "FORT";
            case 4 -> "TRES_FORT";
            default -> "CRITIQUE";
        };
    }
}