package com.project.grcplatform.service;

import com.project.grcplatform.constant.*;
import com.project.grcplatform.dto.KriRequestDTO;
import com.project.grcplatform.dto.KriResponseDTO;
import com.project.grcplatform.exception.AppException;
import com.project.grcplatform.mapper.KriMapper;
import com.project.grcplatform.model.Kri;
import com.project.grcplatform.model.KriCategory;
import com.project.grcplatform.model.KriHistory;
import com.project.grcplatform.repository.KriCategoryRepository;
import com.project.grcplatform.repository.KriHistoryRepository;
import com.project.grcplatform.repository.KriRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KriService {

    private final KriRepository kriRepository;
    private final KriCategoryRepository categoryRepository;
    private final KriHistoryRepository historyRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;


    public Page<KriResponseDTO> findAll(String name, String categoryId, KriStatus status,
                                        KriLinkedEntityType linkedEntityType, String linkedEntityId,
                                        String ownerId, Pageable pageable) {
        return kriRepository
                .findAllWithFilters(name, categoryId, status, linkedEntityType, linkedEntityId, ownerId, pageable)
                .map(KriMapper::toDTO);
    }

    public KriResponseDTO findById(String id) {
        return KriMapper.toDTO(getOrThrow(id));
    }

    public List<KriResponseDTO> getBreached() {
        return kriRepository.findByDeletedFalseAndStatus(KriStatus.BREACH)
                .stream().map(KriMapper::toDTO).toList();
    }

    public Map<String, Long> getSummary() {
        Map<String, Long> summary = new HashMap<>();
        summary.put("NORMAL",  0L);
        summary.put("WARNING", 0L);
        summary.put("BREACH",  0L);
        kriRepository.countByStatus().forEach(row ->
                summary.put(row[0].toString(), (Long) row[1])
        );
        return summary;
    }

    public List<KriHistory> getHistory(String id) {
        getOrThrow(id);
        return historyRepository.findByKri_IdOrderByCreatedAtDesc(id);
    }

    // ─── Write ───────────────────────────────────────────────────────────────────

    @Transactional
    public KriResponseDTO create(KriRequestDTO request) {
        Kri kri = KriMapper.toEntity(request);
        if (request.getCategoryId() != null) {
            KriCategory cat = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.KRI_NOT_FOUND));
            kri.setCategory(cat);
        }
        Kri saved = kriRepository.saveAndFlush(kri);
        Kri fresh = kriRepository.findByIdAndDeletedFalse(saved.getId()).orElse(saved);
        saveHistory(fresh, "KRI created");

        auditService.log(
                AuditAction.KRI_CREATED,
                AuditEntityType.KRI,
                fresh.getId(),
                "KRI created: " + fresh.getName()
        );

        // Notify immediately if created already in WARNING or BREACH state
        notifyIfThresholdBreached(null, fresh);

        return KriMapper.toDTO(fresh);
    }

    @Transactional
    public KriResponseDTO update(String id, KriRequestDTO request) {
        Kri kri = getOrThrow(id);
        KriStatus previousStatus = kri.getStatus();

        Map<String, Object> oldValues = Map.of(
                "name",            kri.getName()            != null ? kri.getName()                   : "",
                "currentValue",    kri.getCurrentValue()    != null ? kri.getCurrentValue().toString() : "",
                "warningThreshold",kri.getWarningThreshold() != null ? kri.getWarningThreshold().toString() : "",
                "breachThreshold", kri.getBreachThreshold() != null ? kri.getBreachThreshold().toString()  : "",
                "status",          kri.getStatus()          != null ? kri.getStatus().name()           : ""
        );

        KriMapper.updateEntity(kri, request);
        if (request.getCategoryId() != null) {
            KriCategory cat = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.KRI_NOT_FOUND));
            kri.setCategory(cat);
        }
        Kri saved = kriRepository.save(kri);
        saveHistory(saved, "KRI updated");

        auditService.log(
                AuditAction.KRI_UPDATED,
                AuditEntityType.KRI,
                saved.getId(),
                "KRI updated: " + saved.getName(),
                oldValues,
                Map.of(
                        "name",            saved.getName()            != null ? saved.getName()                   : "",
                        "currentValue",    saved.getCurrentValue()    != null ? saved.getCurrentValue().toString() : "",
                        "warningThreshold",saved.getWarningThreshold() != null ? saved.getWarningThreshold().toString() : "",
                        "breachThreshold", saved.getBreachThreshold() != null ? saved.getBreachThreshold().toString()  : "",
                        "status",          saved.getStatus()          != null ? saved.getStatus().name()           : ""
                )
        );

        notifyIfThresholdBreached(previousStatus, saved);
        return KriMapper.toDTO(saved);
    }

    @Transactional
    public KriResponseDTO updateValue(String id, Double newValue) {
        Kri kri = getOrThrow(id);
        KriStatus previousStatus = kri.getStatus();
        String oldValue = kri.getCurrentValue() != null ? kri.getCurrentValue().toString() : "";

        kri.setCurrentValue(newValue);
        kri.setLastEvaluatedAt(LocalDateTime.now());
        Kri saved = kriRepository.save(kri);
        saveHistory(saved, "KRI value updated to " + newValue);

        auditService.log(
                AuditAction.KRI_VALUE_UPDATED,
                AuditEntityType.KRI,
                saved.getId(),
                "KRI value updated: " + saved.getName() + " → " + newValue,
                Map.of("currentValue", oldValue, "status", previousStatus != null ? previousStatus.name() : ""),
                Map.of("currentValue", newValue.toString(), "status", saved.getStatus().name())
        );

        notifyIfThresholdBreached(previousStatus, saved);
        return KriMapper.toDTO(saved);
    }

    @Transactional
    public void delete(String id) {
        Kri kri = getOrThrow(id);
        kri.setDeleted(true);
        kriRepository.save(kri);
        saveHistory(kri, "KRI deleted");

        auditService.log(
                AuditAction.KRI_DELETED,
                AuditEntityType.KRI,
                id,
                "KRI deleted: " + kri.getName()
        );
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    /**
     * Sends a notification when status transitions into WARNING or BREACH.
     * Only fires on a real escalation — not on improvement (BREACH → WARNING, etc.).
     *
     * @param previousStatus status before the change (null if newly created)
     * @param kri            saved KRI with updated status
     */
    private void notifyIfThresholdBreached(KriStatus previousStatus, Kri kri) {
        if (kri.getCreatedBy() == null) return;
        KriStatus current = kri.getStatus();

        boolean wasNormal  = previousStatus == null || previousStatus == KriStatus.NORMAL;
        boolean wasWarning = previousStatus == KriStatus.WARNING;

        if (current == KriStatus.BREACH && (wasNormal || wasWarning)) {
            notificationService.notify(
                    kri.getCreatedBy().getId(),
                    NotificationType.KRI_BREACH,
                    NotificationEntityType.KRI,
                    kri.getId(),
                    "🔴 KRI en état BREACH",
                    "L'indicateur \"" + kri.getName() + "\" a atteint le seuil critique. "
                            + "Valeur actuelle : " + kri.getCurrentValue() + " " + (kri.getUnit() != null ? kri.getUnit() : "")
                            + " | Seuil BREACH : " + kri.getBreachThreshold() + "."
            );
        } else if (current == KriStatus.WARNING && wasNormal) {
            notificationService.notify(
                    kri.getCreatedBy().getId(),
                    NotificationType.KRI_WARNING,
                    NotificationEntityType.KRI,
                    kri.getId(),
                    "🟡 KRI en état WARNING",
                    "L'indicateur \"" + kri.getName() + "\" a dépassé le seuil d'avertissement. "
                            + "Valeur actuelle : " + kri.getCurrentValue() + " " + (kri.getUnit() != null ? kri.getUnit() : "")
                            + " | Seuil WARNING : " + kri.getWarningThreshold() + "."
            );
        }
    }

    private Kri getOrThrow(String id) {
        return kriRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.KRI_NOT_FOUND));
    }

    private void saveHistory(Kri kri, String summary) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id",                 kri.getId());
        snapshot.put("name",               kri.getName());
        snapshot.put("description",        kri.getDescription());
        snapshot.put("category",           kri.getCategory()           != null ? kri.getCategory().getName()        : null);
        snapshot.put("linkedEntityType",   kri.getLinkedEntityType()   != null ? kri.getLinkedEntityType().name()   : null);
        snapshot.put("linkedEntityId",     kri.getLinkedEntityId());
        snapshot.put("unit",               kri.getUnit());
        snapshot.put("currentValue",       kri.getCurrentValue());
        snapshot.put("warningThreshold",   kri.getWarningThreshold());
        snapshot.put("breachThreshold",    kri.getBreachThreshold());
        snapshot.put("thresholdDirection", kri.getThresholdDirection() != null ? kri.getThresholdDirection().name() : null);
        snapshot.put("status",             kri.getStatus()             != null ? kri.getStatus().name()             : null);
        snapshot.put("lastEvaluatedAt",    kri.getLastEvaluatedAt()    != null ? kri.getLastEvaluatedAt().toString() : null);
        snapshot.put("createdBy",          kri.getCreatedBy() != null ? kri.getCreatedBy().getId() : null);
        snapshot.put("createdAt",          kri.getCreatedAt()          != null ? kri.getCreatedAt().toString()      : null);
        snapshot.put("updatedAt",          kri.getUpdatedAt()          != null ? kri.getUpdatedAt().toString()      : null);

        KriHistory history = KriHistory.builder()
                .kri(kri)
                .changeSummary(summary)
                .snapshot(snapshot)
                .build();
        historyRepository.save(history);
    }

}