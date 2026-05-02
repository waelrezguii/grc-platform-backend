package com.project.grcplatform.service;

import com.project.grcplatform.constant.*;
import com.project.grcplatform.dto.ThreatRequestDTO;
import com.project.grcplatform.dto.ThreatResponseDTO;
import com.project.grcplatform.dto.ThreatReviewRequestDTO;
import com.project.grcplatform.exception.AppException;
import com.project.grcplatform.exception.NotFoundException;
import com.project.grcplatform.mapper.ThreatMapper;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ThreatService {

    private final ThreatRepository threatRepository;
    private final ThreatHistoryRepository historyRepository;
    private final ThreatTypeRepository threatTypeRepository;
    private final AssetRepository assetRepository;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final AuditService auditService;

    public Page<ThreatResponseDTO> findAll(String name, ThreatOrigin origin,
                                           String typeId, ThreatSeverity severity,
                                           ThreatStatus status, Pageable pageable) {
        return threatRepository
                .findAllWithFilters(name, origin, typeId, severity, status, pageable)
                .map(ThreatMapper::toDTO);
    }

    public ThreatResponseDTO findById(String id) {
        return ThreatMapper.toDTO(getOrThrow(id));
    }

    @Transactional
    public ThreatResponseDTO create(ThreatRequestDTO request) {
        Threat threat = ThreatMapper.toEntity(request);

        if (request.getTypeId() != null) {
            ThreatType type = threatTypeRepository.findById(request.getTypeId())
                    .orElseThrow(() -> new NotFoundException("Threat type not found: " + request.getTypeId()));
            threat.setType(type);
        }
        resolveAssociations(threat, request);

        Threat saved = threatRepository.saveAndFlush(threat);
        Threat fresh = threatRepository.findByIdAndDeletedFalse(saved.getId()).orElse(saved);
        saveHistory(fresh, "Threat created");

        auditService.log(
                AuditAction.THREAT_CREATED,
                AuditEntityType.THREAT,
                fresh.getId(),
                "Threat created: " + fresh.getName()
        );

        return ThreatMapper.toDTO(fresh);
    }

    @Transactional
    public ThreatResponseDTO update(String id, ThreatRequestDTO request) {
        Threat threat = getOrThrow(id);
        if (threat.getStatus() == ThreatStatus.VALIDATED) {
            throw new AppException(ErrorCode.THREAT_ALREADY_VALIDATED);
        }

        Map<String, Object> oldValues = Map.of(
                "name",     threat.getName()     != null ? threat.getName()     : "",
                "origin",   threat.getOrigin()   != null ? threat.getOrigin().name()   : "",
                "type",     threat.getType()     != null ? threat.getType().getName()  : "",
                "severity", threat.getSeverity() != null ? threat.getSeverity().name() : ""
        );

        ThreatMapper.updateEntity(threat, request);

        if (request.getTypeId() != null) {
            ThreatType type = threatTypeRepository.findById(request.getTypeId())
                    .orElseThrow(() -> new NotFoundException("Threat type not found: " + request.getTypeId()));
            threat.setType(type);
        }
        resolveAssociations(threat, request);

        Threat saved = threatRepository.save(threat);
        saveHistory(saved, "Threat updated");

        Map<String, Object> newValues = Map.of(
                "name",     saved.getName()     != null ? saved.getName()     : "",
                "origin",   saved.getOrigin()   != null ? saved.getOrigin().name()   : "",
                "type",     saved.getType()     != null ? saved.getType().getName()  : "",
                "severity", saved.getSeverity() != null ? saved.getSeverity().name() : ""
        );

        auditService.log(
                AuditAction.THREAT_UPDATED,
                AuditEntityType.THREAT,
                saved.getId(),
                "Threat updated: " + saved.getName(),
                oldValues,
                newValues
        );

        return ThreatMapper.toDTO(saved);
    }

    @Transactional
    public ThreatResponseDTO validate(String id) {
        Threat threat = getOrThrow(id);
        if (threat.getStatus() == ThreatStatus.VALIDATED) {
            throw new AppException(ErrorCode.THREAT_ALREADY_VALIDATED);
        }
        assertProfileComplete(threat);

        threat.setStatus(ThreatStatus.VALIDATED);
        threat.setValidatedBy(getCurrentUserId());
        threat.setValidatedAt(LocalDateTime.now());
        Threat saved = threatRepository.save(threat);
        saveHistory(saved, "Threat validated");

        auditService.log(
                AuditAction.THREAT_VALIDATED,
                AuditEntityType.THREAT,
                saved.getId(),
                "Threat validated: " + saved.getName()
        );

        return ThreatMapper.toDTO(saved);
    }

    @Transactional
    public ThreatResponseDTO review(String id, ThreatReviewRequestDTO request) {
        Threat threat = getOrThrow(id);

        threat.setLastReviewedBy(getCurrentUserId());
        threat.setLastReviewedAt(LocalDateTime.now());
        threat.setReviewNotes(request.getNotes());
        threat.setNextReviewDue(
                request.getNextReviewDue() != null
                        ? request.getNextReviewDue()
                        : LocalDateTime.now().plusMonths(6)
        );

        Threat saved = threatRepository.save(threat);
        saveHistory(saved, "Threat profile reviewed");

        auditService.log(
                AuditAction.THREAT_REVIEWED,
                AuditEntityType.THREAT,
                saved.getId(),
                "Threat profile reviewed: " + saved.getName()
        );

        return ThreatMapper.toDTO(saved);
    }

    @Transactional
    public void delete(String id) {
        Threat threat = getOrThrow(id);
        threat.setDeleted(true);
        threatRepository.save(threat);
        saveHistory(threat, "Threat deleted");

        auditService.log(
                AuditAction.THREAT_DELETED,
                AuditEntityType.THREAT,
                id,
                "Threat deleted: " + threat.getName()
        );
    }

    public List<ThreatHistory> getHistory(String id) {
        getOrThrow(id);
        return historyRepository.findByThreat_IdOrderByCreatedAtDesc(id);
    }

    // -------------------------

    /**
     * Ensures the minimum mandatory profile fields are filled before validation.
     * Required: description, scenario, origin, vectors.
     */
    private void assertProfileComplete(Threat threat) {
        boolean incomplete =
                isBlank(threat.getDescription()) ||
                isBlank(threat.getScenario())     ||
                threat.getOrigin() == null        ||
                isBlank(threat.getVectors());
        if (incomplete) {
            throw new AppException(ErrorCode.THREAT_PROFILE_INCOMPLETE);
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** Replaces the threat's asset and vulnerability associations from the request. */
    private void resolveAssociations(Threat threat, ThreatRequestDTO request) {
        if (request.getAssetIds() != null) {
            List<Asset> assets = new ArrayList<>(
                    assetRepository.findAllById(request.getAssetIds())
            );
            threat.setAssets(assets);
        }
        if (request.getVulnerabilityIds() != null) {
            List<Vulnerability> vulns = new ArrayList<>(
                    vulnerabilityRepository.findAllById(request.getVulnerabilityIds())
            );
            threat.setVulnerabilities(vulns);
        }
    }

    private Threat getOrThrow(String id) {
        return threatRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.THREAT_NOT_FOUND));
    }

    private void saveHistory(Threat threat, String summary) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", threat.getId());
        snapshot.put("name", threat.getName());
        snapshot.put("description", threat.getDescription());
        snapshot.put("origin", threat.getOrigin() != null ? threat.getOrigin().name() : null);
        snapshot.put("type", threat.getType() != null ? threat.getType().getName() : null);
        snapshot.put("frequency", threat.getFrequency() != null ? threat.getFrequency().name() : null);
        snapshot.put("severity", threat.getSeverity() != null ? threat.getSeverity().name() : null);
        snapshot.put("motivation", threat.getMotivation());
        snapshot.put("objectives", threat.getObjectives());
        snapshot.put("scenario", threat.getScenario());
        snapshot.put("vectors", threat.getVectors());
        snapshot.put("aggravatingFactors", threat.getAggravatingFactors());
        snapshot.put("status", threat.getStatus() != null ? threat.getStatus().name() : null);
        snapshot.put("validatedBy", threat.getValidatedBy());
        snapshot.put("validatedAt", threat.getValidatedAt() != null ? threat.getValidatedAt().toString() : null);
        snapshot.put("lastReviewedBy", threat.getLastReviewedBy());
        snapshot.put("lastReviewedAt", threat.getLastReviewedAt() != null ? threat.getLastReviewedAt().toString() : null);
        snapshot.put("nextReviewDue", threat.getNextReviewDue() != null ? threat.getNextReviewDue().toString() : null);
        snapshot.put("reviewNotes", threat.getReviewNotes());
        snapshot.put("assetIds", threat.getAssets() != null
                ? threat.getAssets().stream().map(a -> a.getId()).toList() : List.of());
        snapshot.put("vulnerabilityIds", threat.getVulnerabilities() != null
                ? threat.getVulnerabilities().stream().map(v -> v.getId()).toList() : List.of());
        snapshot.put("createdBy", threat.getCreatedBy() != null ? threat.getCreatedBy().getId() : null);
        snapshot.put("createdAt", threat.getCreatedAt() != null ? threat.getCreatedAt().toString() : null);
        snapshot.put("updatedAt", threat.getUpdatedAt() != null ? threat.getUpdatedAt().toString() : null);

        ThreatHistory history = ThreatHistory.builder()
                .threat(threat)
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
