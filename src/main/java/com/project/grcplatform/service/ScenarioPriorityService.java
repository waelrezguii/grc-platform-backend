package com.project.grcplatform.service;

import com.project.grcplatform.constant.*;
import com.project.grcplatform.dto.ScenarioPriorityConfigRequestDTO;
import com.project.grcplatform.dto.ScenarioPriorityConfigResponseDTO;
import com.project.grcplatform.exception.AppException;
import com.project.grcplatform.model.RiskScenario;
import com.project.grcplatform.model.ScenarioPriorityConfig;
import com.project.grcplatform.model.ScenarioPriorityConfigHistory;
import com.project.grcplatform.repository.RiskScenarioRepository;
import com.project.grcplatform.repository.ScenarioPriorityConfigHistoryRepository;
import com.project.grcplatform.repository.ScenarioPriorityConfigRepository;
import com.project.grcplatform.security.JwtAuthToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScenarioPriorityService {

    private final ScenarioPriorityConfigRepository configRepository;
    private final ScenarioPriorityConfigHistoryRepository historyRepository;
    private final RiskScenarioRepository scenarioRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    // ── Config CRUD ───────────────────────────────────────────────────────────

    public Page<ScenarioPriorityConfigResponseDTO> findAll(String name, Pageable pageable) {
        return configRepository.findAllWithFilters(name, pageable).map(this::toDTO);
    }

    public ScenarioPriorityConfigResponseDTO findById(String id) {
        return toDTO(getOrThrow(id));
    }

    @Transactional
    public ScenarioPriorityConfigResponseDTO create(ScenarioPriorityConfigRequestDTO request) {
        validateWeights(request);

        ScenarioPriorityConfig config = ScenarioPriorityConfig.builder()
                .name(request.getName())
                .description(request.getDescription())
                .likelihoodWeight(request.getLikelihoodWeight())
                .impactWeight(request.getImpactWeight())
                .regulatoryExposureWeight(request.getRegulatoryExposureWeight())
                .cvssWeight(request.getCvssWeight())
                .alertThreshold(request.getAlertThreshold() != null ? request.getAlertThreshold() : 75.0)
                .active(false)
                .build();

        ScenarioPriorityConfig saved = configRepository.save(config);
        saveHistory(saved, "Config created");

        auditService.log(AuditAction.PRIORITY_CONFIG_CREATED, AuditEntityType.PRIORITY_CONFIG,
                saved.getId(), "Priority config created: " + saved.getName());

        return toDTO(saved);
    }

    @Transactional
    public ScenarioPriorityConfigResponseDTO update(String id, ScenarioPriorityConfigRequestDTO request) {
        validateWeights(request);
        ScenarioPriorityConfig config = getOrThrow(id);

        if (request.getName() != null)                      config.setName(request.getName());
        if (request.getDescription() != null)               config.setDescription(request.getDescription());
        if (request.getLikelihoodWeight() != null)          config.setLikelihoodWeight(request.getLikelihoodWeight());
        if (request.getImpactWeight() != null)              config.setImpactWeight(request.getImpactWeight());
        if (request.getRegulatoryExposureWeight() != null)  config.setRegulatoryExposureWeight(request.getRegulatoryExposureWeight());
        if (request.getCvssWeight() != null)                config.setCvssWeight(request.getCvssWeight());
        if (request.getAlertThreshold() != null)            config.setAlertThreshold(request.getAlertThreshold());

        ScenarioPriorityConfig saved = configRepository.save(config);
        saveHistory(saved, "Config updated");

        auditService.log(AuditAction.PRIORITY_CONFIG_UPDATED, AuditEntityType.PRIORITY_CONFIG,
                saved.getId(), "Priority config updated: " + saved.getName());

        // If this config is active, recalculate all scenarios
        if (Boolean.TRUE.equals(saved.getActive())) {
            recalculateAll(saved);
        }

        return toDTO(saved);
    }

    @Transactional
    public ScenarioPriorityConfigResponseDTO activate(String id) {
        ScenarioPriorityConfig config = getOrThrow(id);

        // Deactivate all others first
        configRepository.deactivateAllExcept(id);
        config.setActive(true);
        ScenarioPriorityConfig saved = configRepository.save(config);
        saveHistory(saved, "Config activated");

        auditService.log(AuditAction.PRIORITY_CONFIG_ACTIVATED, AuditEntityType.PRIORITY_CONFIG,
                saved.getId(), "Priority config activated: " + saved.getName());

        // Recalculate all scenarios with the newly activated config
        recalculateAll(saved);

        return toDTO(saved);
    }

    @Transactional
    public void delete(String id) {
        ScenarioPriorityConfig config = getOrThrow(id);
        config.setDeleted(true);
        configRepository.save(config);

        auditService.log(AuditAction.PRIORITY_CONFIG_DELETED, AuditEntityType.PRIORITY_CONFIG,
                id, "Priority config deleted: " + config.getName());
    }

    public List<ScenarioPriorityConfigHistory> getHistory(String id) {
        getOrThrow(id);
        return historyRepository.findByConfig_IdOrderByChangedAtDesc(id);
    }

    // ── Score computation (called from RiskScenarioService on every save) ─────

    /**
     * Computes and sets priorityScore + priorityLevel on the given scenario.
     * Uses the currently active config; falls back to default weights if none is active.
     * Also sends a notification if the score crosses the alert threshold.
     * Does NOT save the scenario — caller is responsible.
     */
    public void recalculate(RiskScenario scenario) {
        Optional<ScenarioPriorityConfig> activeOpt = configRepository.findByActiveTrueAndDeletedFalse();

        double wLikelihood   = activeOpt.map(ScenarioPriorityConfig::getLikelihoodWeight).orElse(0.3);
        double wImpact       = activeOpt.map(ScenarioPriorityConfig::getImpactWeight).orElse(0.3);
        double wRegulatory   = activeOpt.map(ScenarioPriorityConfig::getRegulatoryExposureWeight).orElse(0.2);
        double wCvss         = activeOpt.map(ScenarioPriorityConfig::getCvssWeight).orElse(0.2);
        double alertThreshold = activeOpt.map(ScenarioPriorityConfig::getAlertThreshold).orElse(75.0);

        // Normalise each criterion to [0, 1] then scale by 100
        double likelihoodNorm   = scenario.getLikelihood()          != null ? (scenario.getLikelihood() - 1) / 3.0       : 0.0;
        double impactNorm       = scenario.getImpact()              != null ? (scenario.getImpact() - 1) / 3.0           : 0.0;
        double regulatoryNorm   = scenario.getRegulatoryExposure()  != null ? (scenario.getRegulatoryExposure() - 1) / 3.0 : 0.0;

        double cvssNorm = 0.0;
        if (scenario.getVulnerability() != null && scenario.getVulnerability().getCvssScore() != null) {
            cvssNorm = scenario.getVulnerability().getCvssScore().doubleValue() / 10.0;
        }

        double score = (wLikelihood * likelihoodNorm
                + wImpact     * impactNorm
                + wRegulatory * regulatoryNorm
                + wCvss       * cvssNorm) * 100.0;

        // Clamp to [0, 100] and round to one decimal
        score = Math.min(100.0, Math.max(0.0, score));
        score = Math.round(score * 10.0) / 10.0;

        boolean wasBelow = scenario.getPriorityScore() == null || scenario.getPriorityScore() < alertThreshold;

        scenario.setPriorityScore(score);
        scenario.setPriorityLevel(toPriorityLevel(score));

        // Alert notification when the score reaches or crosses the threshold
        if (wasBelow && score >= alertThreshold && scenario.getOwner() != null) {
            notificationService.notify(
                    scenario.getOwner().getId(),
                    NotificationType.SCENARIO_PRIORITY_ALERT,
                    NotificationEntityType.SCENARIO,
                    scenario.getId(),
                    "Priority alert: " + scenario.getName(),
                    String.format("Scenario \"%s\" has reached a priority score of %.1f (%s).",
                            scenario.getName(), score, scenario.getPriorityLevel())
            );
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Batch recalculate all non-deleted scenarios and flush. */
    private void recalculateAll(ScenarioPriorityConfig config) {
        log.info("Batch recalculating scenario priorities with config '{}'", config.getName());
        scenarioRepository.findAll().stream()
                .filter(s -> !Boolean.TRUE.equals(s.getDeleted()))
                .forEach(s -> {
                    recalculate(s);
                    scenarioRepository.save(s);
                });
    }

    private static PriorityLevel toPriorityLevel(double score) {
        if (score <= 25.0)  return PriorityLevel.LOW;
        if (score <= 50.0)  return PriorityLevel.MEDIUM;
        if (score <= 75.0)  return PriorityLevel.HIGH;
        return PriorityLevel.CRITICAL;
    }

    private void validateWeights(ScenarioPriorityConfigRequestDTO req) {
        if (req.getLikelihoodWeight() == null || req.getImpactWeight() == null
                || req.getRegulatoryExposureWeight() == null || req.getCvssWeight() == null) {
            return; // individual @NotNull on DTO handles this for create; PATCH allows nulls
        }
        double sum = req.getLikelihoodWeight() + req.getImpactWeight()
                + req.getRegulatoryExposureWeight() + req.getCvssWeight();
        // Allow ±0.001 tolerance for floating-point
        if (Math.abs(sum - 1.0) > 0.001) {
            throw new AppException(ErrorCode.PRIORITY_CONFIG_WEIGHTS_INVALID);
        }
    }

    private ScenarioPriorityConfig getOrThrow(String id) {
        return configRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRIORITY_CONFIG_NOT_FOUND));
    }

    private void saveHistory(ScenarioPriorityConfig config, String summary) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", config.getId());
        snapshot.put("name", config.getName());
        snapshot.put("description", config.getDescription());
        snapshot.put("likelihoodWeight", config.getLikelihoodWeight());
        snapshot.put("impactWeight", config.getImpactWeight());
        snapshot.put("regulatoryExposureWeight", config.getRegulatoryExposureWeight());
        snapshot.put("cvssWeight", config.getCvssWeight());
        snapshot.put("alertThreshold", config.getAlertThreshold());
        snapshot.put("active", config.getActive());

        ScenarioPriorityConfigHistory h = ScenarioPriorityConfigHistory.builder()
                .config(config)
                .changedBy(getCurrentUserId())
                .changedAt(LocalDateTime.now())
                .changeSummary(summary)
                .snapshot(snapshot)
                .build();
        historyRepository.save(h);
    }

    private ScenarioPriorityConfigResponseDTO toDTO(ScenarioPriorityConfig c) {
        ScenarioPriorityConfigResponseDTO dto = new ScenarioPriorityConfigResponseDTO();
        dto.setId(c.getId());
        dto.setName(c.getName());
        dto.setDescription(c.getDescription());
        dto.setLikelihoodWeight(c.getLikelihoodWeight());
        dto.setImpactWeight(c.getImpactWeight());
        dto.setRegulatoryExposureWeight(c.getRegulatoryExposureWeight());
        dto.setCvssWeight(c.getCvssWeight());
        dto.setAlertThreshold(c.getAlertThreshold());
        dto.setActive(c.getActive());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setUpdatedAt(c.getUpdatedAt());
        return dto;
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthToken jwtAuthToken) return jwtAuthToken.getUserId();
        return "system";
    }
}
