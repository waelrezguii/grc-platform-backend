package com.project.grcplatform.service;

import com.project.grcplatform.constant.*;
import com.project.grcplatform.dto.ControlRequestDTO;
import com.project.grcplatform.dto.ControlResponseDTO;
import com.project.grcplatform.dto.ReviewRequestDTO;
import com.project.grcplatform.exception.AppException;
import com.project.grcplatform.exception.NotFoundException;
import com.project.grcplatform.mapper.ControlMapper;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ControlService {

    private final ControlRepository controlRepository;
    private final ControlHistoryRepository historyRepository;
    private final ControlCategoryRepository controlCategoryRepository;
    private final ControlTypeRepository controlTypeRepository;
    private final RiskScenarioRepository scenarioRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public Page<ControlResponseDTO> findAll(String title, String isoReference,
                                            String categoryId, String typeId,
                                            ControlStatus status, ControlEffectiveness effectiveness,
                                            String assetId, String scenarioId, String ownerId,
                                            Pageable pageable) {
        return controlRepository
                .findAllWithFilters(title, isoReference, categoryId, typeId, status, effectiveness,
                        assetId, scenarioId, ownerId, pageable)
                .map(ControlMapper::toDTO);
    }

    public ControlResponseDTO findById(String id) {
        return ControlMapper.toDTO(getOrThrow(id));
    }

    @Transactional
    public ControlResponseDTO create(ControlRequestDTO request) {
        Control control = ControlMapper.toEntity(request);

        if (request.getCategoryId() != null) {
            ControlCategory category = controlCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Control category not found: " + request.getCategoryId()));
            control.setCategory(category);
        }
        if (request.getTypeId() != null) {
            ControlType type = controlTypeRepository.findById(request.getTypeId())
                    .orElseThrow(() -> new NotFoundException("Control type not found: " + request.getTypeId()));
            control.setType(type);
        }
        if (request.getOwnerId() != null) {
            userRepository.findById(request.getOwnerId()).ifPresent(control::setOwner);
        }
        if (request.getScenarioIds() != null && !request.getScenarioIds().isEmpty()) {
            control.setScenarios(resolveScenarios(request.getScenarioIds()));
        }

        control.computeEfficacy();
        Control saved = controlRepository.saveAndFlush(control);
        Control fresh = controlRepository.findByIdAndDeletedFalse(saved.getId()).orElse(saved);
        saveHistory(fresh, "Control created");

        auditService.log(
                AuditAction.CONTROL_CREATED,
                AuditEntityType.CONTROL,
                fresh.getId(),
                "Control created: " + fresh.getTitle() + " [" + fresh.getIsoReference() + "]"
        );

        return ControlMapper.toDTO(fresh);
    }

    @Transactional
    public ControlResponseDTO update(String id, ControlRequestDTO request) {
        Control control = getOrThrow(id);
        if (control.getStatus() == ControlStatus.RETIRED) {
            throw new AppException(ErrorCode.CONTROL_NOT_EDITABLE);
        }

        Map<String, Object> oldValues = Map.of(
                "title",         control.getTitle()         != null ? control.getTitle()         : "",
                "isoReference",  control.getIsoReference()  != null ? control.getIsoReference()  : "",
                "category",      control.getCategory()      != null ? control.getCategory().getName()      : "",
                "type",          control.getType()          != null ? control.getType().getName()          : "",
                "status",        control.getStatus()        != null ? control.getStatus().name()        : "",
                "effectiveness", control.getEffectiveness() != null ? control.getEffectiveness().name() : ""
        );

        ControlMapper.updateEntity(control, request);

        if (request.getCategoryId() != null) {
            ControlCategory category = controlCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Control category not found: " + request.getCategoryId()));
            control.setCategory(category);
        }
        if (request.getTypeId() != null) {
            ControlType type = controlTypeRepository.findById(request.getTypeId())
                    .orElseThrow(() -> new NotFoundException("Control type not found: " + request.getTypeId()));
            control.setType(type);
        }
        if (request.getOwnerId() != null) {
            userRepository.findById(request.getOwnerId()).ifPresent(control::setOwner);
        }
        if (request.getScenarioIds() != null) {
            control.setScenarios(resolveScenarios(request.getScenarioIds()));
        }

        control.computeEfficacy();
        Control saved = controlRepository.save(control);
        saveHistory(saved, "Control updated");

        Map<String, Object> newValues = Map.of(
                "title",         saved.getTitle()         != null ? saved.getTitle()         : "",
                "isoReference",  saved.getIsoReference()  != null ? saved.getIsoReference()  : "",
                "category",      saved.getCategory()      != null ? saved.getCategory().getName()      : "",
                "type",          saved.getType()          != null ? saved.getType().getName()          : "",
                "status",        saved.getStatus()        != null ? saved.getStatus().name()        : "",
                "effectiveness", saved.getEffectiveness() != null ? saved.getEffectiveness().name() : ""
        );

        auditService.log(
                AuditAction.CONTROL_UPDATED,
                AuditEntityType.CONTROL,
                saved.getId(),
                "Control updated: " + saved.getTitle() + " [" + saved.getIsoReference() + "]",
                oldValues,
                newValues
        );

        return ControlMapper.toDTO(saved);
    }

    // --- Lifecycle ---

    @Transactional
    public ControlResponseDTO implement(String id) {
        Control control = getOrThrow(id);
        if (control.getStatus() != ControlStatus.PLANNED) {
            throw new AppException(ErrorCode.CONTROL_INVALID_TRANSITION);
        }
        control.setStatus(ControlStatus.IMPLEMENTED);
        Control saved = controlRepository.save(control);
        saveHistory(saved, "Control marked as implemented");

        auditService.log(
                AuditAction.CONTROL_IMPLEMENTED,
                AuditEntityType.CONTROL,
                saved.getId(),
                "Control implemented: " + saved.getTitle() + " [" + saved.getIsoReference() + "]"
        );

        return ControlMapper.toDTO(saved);
    }

    @Transactional
    public ControlResponseDTO review(String id, ReviewRequestDTO request) {
        Control control = getOrThrow(id);
        if (control.getStatus() == ControlStatus.RETIRED) {
            throw new AppException(ErrorCode.CONTROL_NOT_EDITABLE);
        }

        String oldEffectiveness = control.getEffectiveness() != null ? control.getEffectiveness().name() : "NOT_REVIEWED";
        String oldStatus        = control.getStatus()        != null ? control.getStatus().name()        : "";

        control.setEffectiveness(request.getEffectiveness());
        control.setStatus(request.getStatus());
        control.setLastReviewDate(LocalDate.now());
        if (request.getNextReviewDate()      != null) control.setNextReviewDate(request.getNextReviewDate());
        if (request.getImplementationNotes() != null) control.setImplementationNotes(request.getImplementationNotes());
        if (request.getEvidenceUrl()         != null) control.setEvidenceUrl(request.getEvidenceUrl());

        Control saved = controlRepository.save(control);
        saveHistory(saved, "Control reviewed — effectiveness: " + request.getEffectiveness().name());

        auditService.log(
                AuditAction.CONTROL_REVIEWED,
                AuditEntityType.CONTROL,
                saved.getId(),
                "Control reviewed: " + saved.getTitle() + " — effectiveness: " + request.getEffectiveness().name(),
                Map.of("effectiveness", oldEffectiveness, "status", oldStatus),
                Map.of("effectiveness", saved.getEffectiveness().name(), "status", saved.getStatus().name())
        );

        return ControlMapper.toDTO(saved);
    }

    @Transactional
    public ControlResponseDTO retire(String id) {
        Control control = getOrThrow(id);
        if (control.getStatus() == ControlStatus.RETIRED) {
            throw new AppException(ErrorCode.CONTROL_INVALID_TRANSITION);
        }
        control.setStatus(ControlStatus.RETIRED);
        Control saved = controlRepository.save(control);
        saveHistory(saved, "Control retired");

        auditService.log(
                AuditAction.CONTROL_RETIRED,
                AuditEntityType.CONTROL,
                saved.getId(),
                "Control retired: " + saved.getTitle() + " [" + saved.getIsoReference() + "]"
        );

        return ControlMapper.toDTO(saved);
    }

    @Transactional
    public ControlResponseDTO validate(String id) {
        Control control = getOrThrow(id);
        if (control.getStatus() == ControlStatus.RETIRED) {
            throw new AppException(ErrorCode.CONTROL_NOT_EDITABLE);
        }
        control.setValidatedBy(getCurrentUserId());
        control.setValidatedAt(LocalDateTime.now());
        Control saved = controlRepository.save(control);
        saveHistory(saved, "Control validated by security referent");

        auditService.log(
                AuditAction.CONTROL_VALIDATED,
                AuditEntityType.CONTROL,
                saved.getId(),
                "Control validated: " + saved.getTitle()
        );

        return ControlMapper.toDTO(saved);
    }

    public List<ControlResponseDTO> export(String categoryId, String typeId,
                                           ControlStatus status, ControlEffectiveness effectiveness,
                                           String assetId, String scenarioId) {
        return controlRepository
                .findAllWithFilters(null, null, categoryId, typeId, status, effectiveness,
                        assetId, scenarioId, null, Pageable.unpaged())
                .stream()
                .map(ControlMapper::toDTO)
                .toList();
    }

    @Transactional
    public void delete(String id) {
        Control control = getOrThrow(id);
        if (control.getStatus() == ControlStatus.IMPLEMENTED) {
            throw new AppException(ErrorCode.CONTROL_NOT_EDITABLE);
        }
        control.setDeleted(true);
        controlRepository.save(control);
        saveHistory(control, "Control deleted");

        auditService.log(
                AuditAction.CONTROL_DELETED,
                AuditEntityType.CONTROL,
                id,
                "Control deleted: " + control.getTitle() + " [" + control.getIsoReference() + "]"
        );
    }

    public List<ControlHistory> getHistory(String id) {
        getOrThrow(id);
        return historyRepository.findByControl_IdOrderByCreatedAtDesc(id);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<RiskScenario> resolveScenarios(List<String> ids) {
        if (ids == null || ids.isEmpty()) return new ArrayList<>();
        return ids.stream()
                .map(sid -> scenarioRepository.findByIdAndDeletedFalse(sid)
                        .orElseThrow(() -> new AppException(ErrorCode.SCENARIO_NOT_FOUND)))
                .toList();
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthToken jwtAuthToken) return jwtAuthToken.getUserId();
        return "system";
    }

    private Control getOrThrow(String id) {
        return controlRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.CONTROL_NOT_FOUND));
    }

    private void saveHistory(Control control, String summary) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", control.getId());
        snapshot.put("isoReference", control.getIsoReference());
        snapshot.put("title", control.getTitle());
        snapshot.put("description", control.getDescription());
        snapshot.put("category", control.getCategory() != null ? control.getCategory().getName() : null);
        snapshot.put("type", control.getType() != null ? control.getType().getName() : null);
        snapshot.put("status", control.getStatus() != null ? control.getStatus().name() : null);
        snapshot.put("effectiveness", control.getEffectiveness() != null ? control.getEffectiveness().name() : null);
        snapshot.put("formalism", control.getFormalism() != null ? control.getFormalism().name() : null);
        snapshot.put("nature", control.getNature() != null ? control.getNature().name() : null);
        snapshot.put("timing", control.getTiming() != null ? control.getTiming().name() : null);
        snapshot.put("tracabilite", control.getTracabilite());
        snapshot.put("conformite4Yeux", control.getConformite4Yeux());
        snapshot.put("performance", control.getPerformance() != null ? control.getPerformance().name() : null);
        snapshot.put("efficacy", control.getEfficacy() != null ? control.getEfficacy().name() : null);
        snapshot.put("ownerId", control.getOwner() != null ? control.getOwner().getId() : null);
        snapshot.put("scope", control.getScope());
        snapshot.put("assetId", control.getAsset() != null ? control.getAsset().getId() : null);
        snapshot.put("scenarioIds", control.getScenarios().stream().map(s -> s.getId()).toList());
        snapshot.put("validatedBy", control.getValidatedBy());
        snapshot.put("validatedAt", control.getValidatedAt() != null ? control.getValidatedAt().toString() : null);
        snapshot.put("createdById", control.getCreatedBy() != null ? control.getCreatedBy().getId() : null);
        snapshot.put("lastReviewDate", control.getLastReviewDate() != null ? control.getLastReviewDate().toString() : null);
        snapshot.put("nextReviewDate", control.getNextReviewDate() != null ? control.getNextReviewDate().toString() : null);
        snapshot.put("implementationNotes", control.getImplementationNotes());
        snapshot.put("evidenceUrl", control.getEvidenceUrl());
        snapshot.put("createdBy", control.getCreatedBy() != null ? control.getCreatedBy().getId() : null);
        snapshot.put("createdAt", control.getCreatedAt() != null ? control.getCreatedAt().toString() : null);
        snapshot.put("updatedAt", control.getUpdatedAt() != null ? control.getUpdatedAt().toString() : null);

        ControlHistory history = ControlHistory.builder()
                .control(control)
                .changeSummary(summary)
                .snapshot(snapshot)
                .build();
        historyRepository.save(history);
    }

}
