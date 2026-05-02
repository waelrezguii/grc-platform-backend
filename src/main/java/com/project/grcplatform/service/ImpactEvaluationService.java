package com.project.grcplatform.service;

import com.project.grcplatform.constant.AuditAction;
import com.project.grcplatform.constant.AuditEntityType;
import com.project.grcplatform.constant.ErrorCode;
import com.project.grcplatform.dto.ImpactEvaluationRequestDTO;
import com.project.grcplatform.dto.ImpactEvaluationResponseDTO;
import com.project.grcplatform.exception.AppException;
import com.project.grcplatform.mapper.ImpactEvaluationMapper;
import com.project.grcplatform.model.*;
import com.project.grcplatform.repository.*;
import com.project.grcplatform.security.JwtAuthToken;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImpactEvaluationService {

    private final ImpactEvaluationRepository evaluationRepository;
    private final ImpactEvaluationHistoryRepository historyRepository;
    private final AuditService auditService;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final RiskScenarioRepository scenarioRepository;

    public Page<ImpactEvaluationResponseDTO> findAll(String vulnerabilityId, String scenarioId, Pageable pageable) {
        return evaluationRepository
                .findAllWithFilters(vulnerabilityId, scenarioId, pageable)
                .map(ImpactEvaluationMapper::toDTO);
    }

    public ImpactEvaluationResponseDTO findById(String id) {
        return ImpactEvaluationMapper.toDTO(getOrThrow(id));
    }

    @Transactional
    public ImpactEvaluationResponseDTO create(ImpactEvaluationRequestDTO request) {
        if (request.getOverrideScore() != null &&
                (request.getOverrideJustification() == null || request.getOverrideJustification().isBlank())) {
            throw new AppException(ErrorCode.IMPACT_EVALUATION_OVERRIDE_NO_JUSTIFICATION);
        }

        Vulnerability vuln = vulnerabilityRepository
                .findByIdAndDeletedFalse(request.getVulnerabilityId())
                .orElseThrow(() -> new AppException(ErrorCode.VULNERABILITY_NOT_FOUND));

        RiskScenario scenario = scenarioRepository
                .findByIdAndDeletedFalse(request.getScenarioId())
                .orElseThrow(() -> new AppException(ErrorCode.SCENARIO_NOT_FOUND));

        BigDecimal cvssScore = vuln.getCvssScore();
        Short assetCriticality = vuln.getAsset() != null ? vuln.getAsset().getCriticalityScore() : null;

        BigDecimal computedScore = computeScore(cvssScore, assetCriticality, request.getExploitationContext());

        ImpactEvaluation.ImpactEvaluationBuilder builder = ImpactEvaluation.builder()
                .vulnerability(vuln)
                .scenario(scenario)
                .exploitationContext(request.getExploitationContext())
                .cvssScoreUsed(cvssScore)
                .assetCriticalityUsed(assetCriticality)
                .computedScore(computedScore);

        if (request.getOverrideScore() != null) {
            builder.overrideScore(request.getOverrideScore().setScale(1, RoundingMode.HALF_UP))
                   .overrideJustification(request.getOverrideJustification())
                   .overriddenBy(getCurrentUserId())
                   .overriddenAt(LocalDateTime.now());
        }

        ImpactEvaluation saved = evaluationRepository.saveAndFlush(builder.build());
        ImpactEvaluation fresh = evaluationRepository.findByIdAndDeletedFalse(saved.getId()).orElse(saved);
        saveHistory(fresh, "Impact evaluation created");

        auditService.log(
                AuditAction.IMPACT_EVALUATION_CREATED,
                AuditEntityType.IMPACT_EVALUATION,
                fresh.getId(),
                "Impact evaluation created for vulnerability: " + vuln.getTitle()
        );

        return ImpactEvaluationMapper.toDTO(fresh);
    }

    @Transactional
    public ImpactEvaluationResponseDTO update(String id, ImpactEvaluationRequestDTO request) {
        ImpactEvaluation evaluation = getOrThrow(id);

        if (request.getOverrideScore() != null &&
                (request.getOverrideJustification() == null || request.getOverrideJustification().isBlank())) {
            throw new AppException(ErrorCode.IMPACT_EVALUATION_OVERRIDE_NO_JUSTIFICATION);
        }

        Map<String, Object> oldValues = Map.of(
                "exploitationContext", evaluation.getExploitationContext(),
                "computedScore",       evaluation.getComputedScore() != null ? evaluation.getComputedScore().toPlainString() : "",
                "overrideScore",       evaluation.getOverrideScore() != null ? evaluation.getOverrideScore().toPlainString() : ""
        );

        boolean recompute = false;

        if (request.getExploitationContext() != null) {
            evaluation.setExploitationContext(request.getExploitationContext());
            recompute = true;
        }

        if (recompute) {
            BigDecimal newScore = computeScore(
                    evaluation.getCvssScoreUsed(),
                    evaluation.getAssetCriticalityUsed(),
                    evaluation.getExploitationContext()
            );
            evaluation.setComputedScore(newScore);
        }

        String changeSummary = "Impact evaluation updated";

        if (request.getOverrideScore() != null) {
            evaluation.setOverrideScore(request.getOverrideScore().setScale(1, RoundingMode.HALF_UP));
            evaluation.setOverrideJustification(request.getOverrideJustification());
            evaluation.setOverriddenBy(getCurrentUserId());
            evaluation.setOverriddenAt(LocalDateTime.now());
            changeSummary = "Impact evaluation override applied";
        } else if (request.getOverrideScore() == null && request.getOverrideJustification() != null
                && request.getOverrideJustification().isBlank()) {
            // Explicit clear: overrideJustification = "" signals removal of override
            evaluation.setOverrideScore(null);
            evaluation.setOverrideJustification(null);
            evaluation.setOverriddenBy(null);
            evaluation.setOverriddenAt(null);
            changeSummary = "Impact evaluation override cleared";
        }

        ImpactEvaluation saved = evaluationRepository.save(evaluation);
        saveHistory(saved, changeSummary);

        Map<String, Object> newValues = Map.of(
                "exploitationContext", saved.getExploitationContext(),
                "computedScore",       saved.getComputedScore() != null ? saved.getComputedScore().toPlainString() : "",
                "overrideScore",       saved.getOverrideScore() != null ? saved.getOverrideScore().toPlainString() : ""
        );

        AuditAction action = changeSummary.contains("override")
                ? AuditAction.IMPACT_EVALUATION_OVERRIDDEN
                : AuditAction.IMPACT_EVALUATION_UPDATED;

        auditService.log(
                action,
                AuditEntityType.IMPACT_EVALUATION,
                saved.getId(),
                changeSummary,
                oldValues,
                newValues
        );

        return ImpactEvaluationMapper.toDTO(saved);
    }

    @Transactional
    public void delete(String id) {
        ImpactEvaluation evaluation = getOrThrow(id);
        evaluation.setDeleted(true);
        evaluationRepository.save(evaluation);
        saveHistory(evaluation, "Impact evaluation deleted");

        auditService.log(
                AuditAction.IMPACT_EVALUATION_DELETED,
                AuditEntityType.IMPACT_EVALUATION,
                id,
                "Impact evaluation deleted"
        );
    }

    public List<ImpactEvaluationHistory> getHistory(String id) {
        getOrThrow(id);
        return historyRepository.findByEvaluation_IdOrderByChangedAtDesc(id);
    }

    // -------------------------------------------------------------------------

    /**
     * Weighted impact score formula (result: 0.0–10.0, 1 decimal place):
     *   40% CVSS score (0–10)
     *   40% asset criticality normalised to 0–10 (criticalityScore / 16 * 10)
     *   20% exploitation context normalised to 0–10 ((context - 1) / 3 * 10)
     */
    private BigDecimal computeScore(BigDecimal cvssScore, Short assetCriticality, Short exploitationContext) {
        double cvss   = cvssScore != null ? cvssScore.doubleValue() : 0.0;
        double asset  = assetCriticality != null ? (assetCriticality / 16.0 * 10.0) : 0.0;
        double exploit = (exploitationContext - 1) / 3.0 * 10.0;

        double raw = 0.4 * cvss + 0.4 * asset + 0.2 * exploit;
        raw = Math.min(10.0, Math.max(0.0, raw));

        return BigDecimal.valueOf(Math.round(raw * 10) / 10.0).setScale(1, RoundingMode.HALF_UP);
    }

    private ImpactEvaluation getOrThrow(String id) {
        return evaluationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.IMPACT_EVALUATION_NOT_FOUND));
    }

    private void saveHistory(ImpactEvaluation e, String summary) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", e.getId());
        snapshot.put("vulnerabilityId", e.getVulnerability() != null ? e.getVulnerability().getId() : null);
        snapshot.put("scenarioId", e.getScenario() != null ? e.getScenario().getId() : null);
        snapshot.put("exploitationContext", e.getExploitationContext());
        snapshot.put("cvssScoreUsed", e.getCvssScoreUsed() != null ? e.getCvssScoreUsed().toPlainString() : null);
        snapshot.put("assetCriticalityUsed", e.getAssetCriticalityUsed());
        snapshot.put("computedScore", e.getComputedScore() != null ? e.getComputedScore().toPlainString() : null);
        snapshot.put("overrideScore", e.getOverrideScore() != null ? e.getOverrideScore().toPlainString() : null);
        snapshot.put("overrideJustification", e.getOverrideJustification());
        snapshot.put("overriddenBy", e.getOverriddenBy());
        snapshot.put("overriddenAt", e.getOverriddenAt() != null ? e.getOverriddenAt().toString() : null);
        snapshot.put("createdBy", e.getCreatedBy() != null ? e.getCreatedBy().getId() : null);
        snapshot.put("createdAt", e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
        snapshot.put("updatedAt", e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null);

        ImpactEvaluationHistory history = ImpactEvaluationHistory.builder()
                .evaluation(e)
                .changedBy(getCurrentUserId())
                .changeSummary(summary)
                .snapshot(snapshot)
                .build();
        historyRepository.save(history);
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthToken jwtAuthToken) return jwtAuthToken.getUserId();
        return "system";
    }
}
