package com.project.grcplatform.service;

import com.project.grcplatform.constant.*;
import com.project.grcplatform.dto.*;
import com.project.grcplatform.exception.AppException;
import com.project.grcplatform.mapper.RiskScenarioMapper;
import com.project.grcplatform.model.*;
import com.project.grcplatform.repository.*;
import com.project.grcplatform.dto.UserSummaryDTO;
import com.project.grcplatform.security.JwtAuthToken;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RiskScenarioService {

    private final RiskScenarioRepository scenarioRepository;
    private final RiskScenarioHistoryRepository historyRepository;
    private final ScenarioIncidentRepository incidentRepository;
    private final AuditService auditService;
    private final AssetRepository assetRepository;
    private final ThreatRepository threatRepository;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final UserRepository userRepository;
    private final ScenarioPriorityService priorityService;

    public Page<RiskScenarioResponseDTO> findAll(String name, String assetId,
                                                 String threatId, ScenarioStatus status,
                                                 boolean archived,
                                                 Pageable pageable) {
        return scenarioRepository
                .findAllWithFilters(name, assetId, threatId, status, archived, pageable)
                .map(RiskScenarioMapper::toDTO);
    }

    public RiskScenarioResponseDTO findById(String id) {
        return RiskScenarioMapper.toDTO(getOrThrow(id));
    }

    @Transactional
    public RiskScenarioResponseDTO create(RiskScenarioRequestDTO request) {
        RiskScenario scenario = RiskScenarioMapper.toEntity(request);
        Asset asset = assetRepository.findByIdAndDeletedFalse(request.getAssetId()).orElseThrow(() -> new AppException(ErrorCode.ASSET_NOT_FOUND));
        Threat threat = threatRepository.findByIdAndDeletedFalse(request.getThreatId()).orElseThrow(() -> new AppException(ErrorCode.THREAT_NOT_FOUND));
        scenario.setAsset(asset);
        scenario.setThreat(threat);
        if (request.getVulnerabilityId() != null) {
            vulnerabilityRepository.findByIdAndDeletedFalse(request.getVulnerabilityId()).ifPresent(scenario::setVulnerability);
        }
        priorityService.recalculate(scenario);
        RiskScenario saved = scenarioRepository.saveAndFlush(scenario);
        RiskScenario fresh = scenarioRepository.findByIdAndDeletedFalse(saved.getId()).orElse(saved);
        saveHistory(fresh, "Scenario created");

        auditService.log(
                AuditAction.SCENARIO_CREATED,
                AuditEntityType.SCENARIO,
                fresh.getId(),
                "Scenario created: " + fresh.getName()
        );

        return RiskScenarioMapper.toDTO(fresh);
    }

    @Transactional
    public RiskScenarioResponseDTO update(String id, RiskScenarioRequestDTO request) {
        RiskScenario scenario = getOrThrow(id);
        if (scenario.getStatus() == ScenarioStatus.VALIDATED) {
            throw new AppException(ErrorCode.SCENARIO_ALREADY_VALIDATED);
        }

        Map<String, Object> oldValues = Map.of(
                "name",       scenario.getName()       != null ? scenario.getName()       : "",
                "likelihood", scenario.getLikelihood() != null ? scenario.getLikelihood() : "",
                "impact",     scenario.getImpact()     != null ? scenario.getImpact()     : "",
                "status",     scenario.getStatus()     != null ? scenario.getStatus().name() : ""
        );

        RiskScenarioMapper.updateEntity(scenario, request);
        if (request.getAssetId() != null) {
            Asset updAsset = assetRepository.findByIdAndDeletedFalse(request.getAssetId()).orElseThrow(() -> new AppException(ErrorCode.ASSET_NOT_FOUND));
            scenario.setAsset(updAsset);
        }
        if (request.getThreatId() != null) {
            Threat updThreat = threatRepository.findByIdAndDeletedFalse(request.getThreatId()).orElseThrow(() -> new AppException(ErrorCode.THREAT_NOT_FOUND));
            scenario.setThreat(updThreat);
        }
        if (request.getVulnerabilityId() != null) {
            vulnerabilityRepository.findByIdAndDeletedFalse(request.getVulnerabilityId()).ifPresent(scenario::setVulnerability);
        }
        priorityService.recalculate(scenario);
        RiskScenario saved = scenarioRepository.save(scenario);
        saveHistory(saved, "Scenario updated");

        Map<String, Object> newValues = Map.of(
                "name",       saved.getName()       != null ? saved.getName()       : "",
                "likelihood", saved.getLikelihood() != null ? saved.getLikelihood() : "",
                "impact",     saved.getImpact()     != null ? saved.getImpact()     : "",
                "status",     saved.getStatus()     != null ? saved.getStatus().name() : ""
        );

        auditService.log(
                AuditAction.SCENARIO_UPDATED,
                AuditEntityType.SCENARIO,
                saved.getId(),
                "Scenario updated: " + saved.getName(),
                oldValues,
                newValues
        );

        return RiskScenarioMapper.toDTO(saved);
    }

    @Transactional
    public RiskScenarioResponseDTO submitForReview(String id) {
        RiskScenario scenario = getOrThrow(id);
        if (scenario.getStatus() != ScenarioStatus.DRAFT) {
            throw new AppException(ErrorCode.SCENARIO_INVALID_TRANSITION);
        }
        scenario.setStatus(ScenarioStatus.UNDER_REVIEW);
        RiskScenario saved = scenarioRepository.save(scenario);
        saveHistory(saved, "Scenario submitted for review");

        auditService.log(
                AuditAction.SCENARIO_SUBMITTED,
                AuditEntityType.SCENARIO,
                saved.getId(),
                "Scenario submitted for review: " + saved.getName()
        );

        return RiskScenarioMapper.toDTO(saved);
    }

    @Transactional
    public RiskScenarioResponseDTO validate(String id) {
        RiskScenario scenario = getOrThrow(id);
        if (scenario.getStatus() == ScenarioStatus.VALIDATED) {
            throw new AppException(ErrorCode.SCENARIO_ALREADY_VALIDATED);
        }
        assertScenarioComplete(scenario);
        String currentUser = getCurrentUserId();
        scenario.setStatus(ScenarioStatus.VALIDATED);
        scenario.setReviewedBy(currentUser);
        scenario.setReviewedAt(LocalDateTime.now());
        scenario.setValidatedBy(currentUser);
        scenario.setValidatedAt(LocalDateTime.now());
        RiskScenario saved = scenarioRepository.save(scenario);
        saveHistory(saved, "Scenario validated");

        auditService.log(
                AuditAction.SCENARIO_VALIDATED,
                AuditEntityType.SCENARIO,
                saved.getId(),
                "Scenario validated: " + saved.getName()
        );

        return RiskScenarioMapper.toDTO(saved);
    }

    @Transactional
    public RiskScenarioResponseDTO archive(String id) {
        RiskScenario scenario = getOrThrow(id);
        scenario.setStatus(ScenarioStatus.ARCHIVED);
        RiskScenario saved = scenarioRepository.save(scenario);
        saveHistory(saved, "Scenario archived");

        auditService.log(
                AuditAction.SCENARIO_ARCHIVED,
                AuditEntityType.SCENARIO,
                saved.getId(),
                "Scenario archived: " + saved.getName()
        );

        return RiskScenarioMapper.toDTO(saved);
    }

    @Transactional
    public void delete(String id) {
        RiskScenario scenario = getOrThrow(id);
        if (scenario.getStatus() == ScenarioStatus.VALIDATED) {
            throw new AppException(ErrorCode.SCENARIO_ALREADY_VALIDATED);
        }
        scenario.setDeleted(true);
        scenarioRepository.save(scenario);
        saveHistory(scenario, "Scenario deleted");

        auditService.log(
                AuditAction.SCENARIO_DELETED,
                AuditEntityType.SCENARIO,
                id,
                "Scenario deleted: " + scenario.getName()
        );
    }

    @Transactional
    public RiskScenarioResponseDTO review(String id, RiskScenarioReviewRequestDTO request) {
        RiskScenario scenario = getOrThrow(id);

        scenario.setLastReviewedBy(getCurrentUserId());
        scenario.setLastReviewedAt(LocalDateTime.now());
        scenario.setReviewNotes(request.getNotes());
        scenario.setNextReviewDue(
                request.getNextReviewDue() != null
                        ? request.getNextReviewDue()
                        : LocalDateTime.now().plusYears(1)
        );

        RiskScenario saved = scenarioRepository.save(scenario);
        saveHistory(saved, "Scenario reviewed");

        auditService.log(
                AuditAction.SCENARIO_REVIEWED,
                AuditEntityType.SCENARIO,
                saved.getId(),
                "Scenario reviewed: " + saved.getName()
        );

        return RiskScenarioMapper.toDTO(saved);
    }

    public List<RiskScenarioHistory> getHistory(String id) {
        getOrThrow(id);
        return historyRepository.findByScenario_IdOrderByVersionNumberAsc(id);
    }

    public RiskScenarioHistory getHistoryEntry(String id, String historyId) {
        getOrThrow(id);
        return historyRepository.findById(historyId)
                .filter(h -> h.getScenario().getId().equals(id))
                .orElseThrow(() -> new AppException(ErrorCode.SCENARIO_NOT_FOUND));
    }

    // ── Incidents ─────────────────────────────────────────────────────────────

    public List<ScenarioIncidentResponseDTO> getIncidents(String scenarioId) {
        getOrThrow(scenarioId);
        return incidentRepository.findByScenario_IdOrderByOccurredAtDesc(scenarioId)
                .stream().map(this::toIncidentDTO).toList();
    }

    @Transactional
    public ScenarioIncidentResponseDTO createIncident(String scenarioId, ScenarioIncidentRequestDTO request) {
        RiskScenario scenario = getOrThrow(scenarioId);

        ScenarioIncident.ScenarioIncidentBuilder builder = ScenarioIncident.builder()
                .scenario(scenario)
                .title(request.getTitle())
                .description(request.getDescription())
                .severity(request.getSeverity())
                .occurredAt(request.getOccurredAt())
                .resolvedAt(request.getResolvedAt())
                .actualImpact(request.getActualImpact());

        if (request.getReportedById() != null) {
            userRepository.findById(request.getReportedById()).ifPresent(builder::reportedBy);
        }

        ScenarioIncident saved = incidentRepository.save(builder.build());

        // Appear in the scenario history timeline
        saveHistory(scenario, "Incident added: " + saved.getTitle());

        auditService.log(AuditAction.SCENARIO_INCIDENT_CREATED, AuditEntityType.SCENARIO,
                scenarioId, "Incident added to scenario: " + saved.getTitle());

        return toIncidentDTO(saved);
    }

    @Transactional
    public ScenarioIncidentResponseDTO updateIncident(String scenarioId, String incidentId,
                                                      ScenarioIncidentRequestDTO request) {
        getOrThrow(scenarioId);
        ScenarioIncident incident = incidentRepository.findByIdAndScenario_Id(incidentId, scenarioId)
                .orElseThrow(() -> new AppException(ErrorCode.SCENARIO_INCIDENT_NOT_FOUND));

        if (request.getTitle() != null)       incident.setTitle(request.getTitle());
        if (request.getDescription() != null) incident.setDescription(request.getDescription());
        if (request.getSeverity() != null)    incident.setSeverity(request.getSeverity());
        if (request.getOccurredAt() != null)  incident.setOccurredAt(request.getOccurredAt());
        if (request.getResolvedAt() != null)  incident.setResolvedAt(request.getResolvedAt());
        if (request.getActualImpact() != null) incident.setActualImpact(request.getActualImpact());
        if (request.getReportedById() != null) {
            userRepository.findById(request.getReportedById()).ifPresent(incident::setReportedBy);
        }

        ScenarioIncident saved = incidentRepository.save(incident);

        saveHistory(scenarioRepository.findByIdAndDeletedFalse(scenarioId).orElseThrow(),
                "Incident updated: " + saved.getTitle());

        auditService.log(AuditAction.SCENARIO_INCIDENT_UPDATED, AuditEntityType.SCENARIO,
                scenarioId, "Incident updated: " + saved.getTitle());

        return toIncidentDTO(saved);
    }

    @Transactional
    public void deleteIncident(String scenarioId, String incidentId) {
        RiskScenario scenario = getOrThrow(scenarioId);
        ScenarioIncident incident = incidentRepository.findByIdAndScenario_Id(incidentId, scenarioId)
                .orElseThrow(() -> new AppException(ErrorCode.SCENARIO_INCIDENT_NOT_FOUND));

        String title = incident.getTitle();
        incidentRepository.delete(incident);
        saveHistory(scenario, "Incident removed: " + title);

        auditService.log(AuditAction.SCENARIO_INCIDENT_DELETED, AuditEntityType.SCENARIO,
                scenarioId, "Incident removed: " + title);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private ScenarioIncidentResponseDTO toIncidentDTO(ScenarioIncident i) {
        ScenarioIncidentResponseDTO dto = new ScenarioIncidentResponseDTO();
        dto.setId(i.getId());
        if (i.getScenario() != null) {
            dto.setScenarioId(i.getScenario().getId());
            dto.setScenarioName(i.getScenario().getName());
        }
        dto.setTitle(i.getTitle());
        dto.setDescription(i.getDescription());
        dto.setSeverity(i.getSeverity());
        dto.setOccurredAt(i.getOccurredAt());
        dto.setResolvedAt(i.getResolvedAt());
        dto.setActualImpact(i.getActualImpact());
        dto.setReportedBy(UserSummaryDTO.of(i.getReportedBy()));
        dto.setCreatedAt(i.getCreatedAt());
        dto.setUpdatedAt(i.getUpdatedAt());
        return dto;
    }

    // -------------------------

    /**
     * Ensures description and impactJustification are filled before validation.
     */
    private void assertScenarioComplete(RiskScenario scenario) {
        boolean incomplete =
                isBlank(scenario.getDescription()) ||
                isBlank(scenario.getImpactJustification());
        if (incomplete) {
            throw new AppException(ErrorCode.SCENARIO_PROFILE_INCOMPLETE);
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private RiskScenario getOrThrow(String id) {
        return scenarioRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.SCENARIO_NOT_FOUND));
    }

    private void saveHistory(RiskScenario scenario, String summary) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", scenario.getId());
        snapshot.put("name", scenario.getName());
        snapshot.put("description", scenario.getDescription());
        snapshot.put("assetId", scenario.getAsset() != null ? scenario.getAsset().getId() : null);
        snapshot.put("threatId", scenario.getThreat() != null ? scenario.getThreat().getId() : null);
        snapshot.put("vulnerabilityId", scenario.getVulnerability() != null ? scenario.getVulnerability().getId() : null);
        snapshot.put("likelihood", scenario.getLikelihood());
        snapshot.put("impact", scenario.getImpact());
        snapshot.put("rawRiskScore", scenario.getRawRiskScore());
        snapshot.put("controlEvaluation", scenario.getControlEvaluation());
        snapshot.put("residualRiskScore", scenario.getResidualRiskScore());
        snapshot.put("treatmentPlan", scenario.getTreatmentPlan());
        snapshot.put("impactJustification", scenario.getImpactJustification());
        snapshot.put("lessonsLearned", scenario.getLessonsLearned());
        snapshot.put("status", scenario.getStatus() != null ? scenario.getStatus().name() : null);
        snapshot.put("reviewedBy", scenario.getReviewedBy());
        snapshot.put("reviewedAt", scenario.getReviewedAt() != null ? scenario.getReviewedAt().toString() : null);
        snapshot.put("validatedBy", scenario.getValidatedBy());
        snapshot.put("validatedAt", scenario.getValidatedAt() != null ? scenario.getValidatedAt().toString() : null);
        snapshot.put("lastReviewedBy", scenario.getLastReviewedBy());
        snapshot.put("lastReviewedAt", scenario.getLastReviewedAt() != null ? scenario.getLastReviewedAt().toString() : null);
        snapshot.put("nextReviewDue", scenario.getNextReviewDue() != null ? scenario.getNextReviewDue().toString() : null);
        snapshot.put("reviewNotes", scenario.getReviewNotes());
        snapshot.put("regulatoryExposure", scenario.getRegulatoryExposure());
        snapshot.put("priorityScore", scenario.getPriorityScore());
        snapshot.put("priorityLevel", scenario.getPriorityLevel() != null ? scenario.getPriorityLevel().name() : null);
        snapshot.put("createdBy", scenario.getCreatedBy() != null ? scenario.getCreatedBy().getId() : null);
        snapshot.put("createdAt", scenario.getCreatedAt() != null ? scenario.getCreatedAt().toString() : null);
        snapshot.put("updatedAt", scenario.getUpdatedAt() != null ? scenario.getUpdatedAt().toString() : null);

        int nextVersion = (int) historyRepository.countByScenario_Id(scenario.getId()) + 1;

        RiskScenarioHistory history = RiskScenarioHistory.builder()
                .scenario(scenario)
                .versionNumber(nextVersion)
                .changedBy(getCurrentUserId())
                .changeSummary(summary)
                .snapshot(snapshot)
                .build();
        historyRepository.save(history);
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthToken jwtAuthToken) {
            return jwtAuthToken.getUserId();
        }
        return "system";
    }
}