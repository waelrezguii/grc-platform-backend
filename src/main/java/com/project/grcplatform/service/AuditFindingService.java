package com.project.grcplatform.service;

import com.project.grcplatform.constant.*;
import com.project.grcplatform.dto.*;
import com.project.grcplatform.exception.AppException;
import com.project.grcplatform.mapper.AuditFindingMapper;
import com.project.grcplatform.model.*;
import com.project.grcplatform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditFindingService {

    private final AuditFindingRepository         findingRepository;
    private final AuditFindingHistoryRepository   historyRepository;
    private final AuditCampaignRepository         campaignRepository;
    private final AuditService                    auditService;
    private final NotificationService             notificationService;

    public Page<AuditFindingResponseDTO> findAll(String campaignId, FindingType type,
                                                 FindingSeverity severity, FindingStatus status,
                                                 Pageable pageable) {
        return findingRepository.findAllWithFilters(campaignId, type, severity, status, pageable)
                .map(AuditFindingMapper::toDTO);
    }

    public AuditFindingResponseDTO findById(String id) {
        return AuditFindingMapper.toDTO(getOrThrow(id));
    }

    public List<AuditFindingHistory> getHistory(String id) {
        getOrThrow(id);
        return historyRepository.findByFindingIdOrderByCreatedAtDesc(id);
    }

    @Transactional
    public AuditFindingResponseDTO create(AuditFindingRequestDTO request) {
        AuditCampaign campaign = campaignRepository.findByIdAndDeletedFalse(request.getCampaignId())
                .orElseThrow(() -> new AppException(ErrorCode.AUDIT_CAMPAIGN_NOT_FOUND));

        AuditFinding f = AuditFindingMapper.toEntity(request, campaign);
        AuditFinding saved = findingRepository.saveAndFlush(f);
        AuditFinding fresh = findingRepository.findByIdAndDeletedFalse(saved.getId()).orElse(saved);
        saveHistory(fresh, "Finding created");
        auditService.log(AuditAction.AUDIT_FINDING_CREATED, AuditEntityType.AUDIT_FINDING,
                fresh.getId(), "Finding created: " + fresh.getTitle());

        // Notify on critical finding
        if (fresh.getSeverity() == FindingSeverity.CRITICAL) {
            notificationService.notify(
                    fresh.getCreatedBy().getId(),
                    NotificationType.AUDIT_FINDING_CRITICAL,
                    NotificationEntityType.AUDIT_FINDING, fresh.getId(),
                    "Constat critique détecté",
                    "Un constat critique a été enregistré : \"" + fresh.getTitle() + "\"");
        }
        return AuditFindingMapper.toDTO(fresh);
    }

    @Transactional
    public AuditFindingResponseDTO update(String id, AuditFindingRequestDTO request) {
        AuditFinding f = getOrThrow(id);
        if (f.getStatus() == FindingStatus.CLOSED)
            throw new AppException(ErrorCode.AUDIT_FINDING_INVALID_TRANSITION);
        Map<String, Object> old = snapshot(f);
        AuditFindingMapper.updateEntity(f, request);
        AuditFinding saved = findingRepository.save(f);
        saveHistory(saved, "Finding updated");
        auditService.log(AuditAction.AUDIT_FINDING_UPDATED, AuditEntityType.AUDIT_FINDING,
                saved.getId(), "Finding updated", old, snapshot(saved));
        return AuditFindingMapper.toDTO(saved);
    }

    @Transactional
    public AuditFindingResponseDTO updateStatus(String id, FindingStatus newStatus) {
        AuditFinding f = getOrThrow(id);
        validateTransition(f.getStatus(), newStatus);
        Map<String, Object> old = snapshot(f);
        f.setStatus(newStatus);
        AuditFinding saved = findingRepository.save(f);
        saveHistory(saved, "Status changed to " + newStatus);
        auditService.log(AuditAction.AUDIT_FINDING_STATUS_CHANGED, AuditEntityType.AUDIT_FINDING,
                saved.getId(), "Status: " + old.get("status") + " → " + newStatus, old, snapshot(saved));
        return AuditFindingMapper.toDTO(saved);
    }

    @Transactional
    public void delete(String id) {
        AuditFinding f = getOrThrow(id);
        f.setDeleted(true);
        findingRepository.save(f);
        auditService.log(AuditAction.AUDIT_FINDING_DELETED, AuditEntityType.AUDIT_FINDING,
                id, "Finding deleted: " + f.getTitle());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void validateTransition(FindingStatus current, FindingStatus next) {
        boolean valid = switch (current) {
            case OPEN        -> next == FindingStatus.IN_PROGRESS || next == FindingStatus.ACCEPTED;
            case IN_PROGRESS -> next == FindingStatus.RESOLVED || next == FindingStatus.ACCEPTED;
            case RESOLVED    -> next == FindingStatus.CLOSED;
            case ACCEPTED    -> next == FindingStatus.CLOSED;
            case CLOSED      -> false;
        };
        if (!valid) throw new AppException(ErrorCode.AUDIT_FINDING_INVALID_TRANSITION);
    }

    private AuditFinding getOrThrow(String id) {
        return findingRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.AUDIT_FINDING_NOT_FOUND));
    }

    private void saveHistory(AuditFinding f, String summary) {
        historyRepository.save(AuditFindingHistory.builder()
                .findingId(f.getId())
                .changeSummary(summary)
                .snapshot(snapshot(f))
                .build());
    }

    private Map<String, Object> snapshot(AuditFinding f) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", f.getId());
        m.put("campaignId", f.getCampaign() != null ? f.getCampaign().getId() : null);
        m.put("title", f.getTitle());
        m.put("description", f.getDescription());
        m.put("findingType", f.getFindingType() != null ? f.getFindingType().name() : null);
        m.put("severity", f.getSeverity() != null ? f.getSeverity().name() : null);
        m.put("category", f.getCategory());
        m.put("evidence", f.getEvidence());
        m.put("recommendation", f.getRecommendation());
        m.put("status", f.getStatus() != null ? f.getStatus().name() : null);
        m.put("createdById", f.getCreatedBy() != null ? f.getCreatedBy().getId() : null);
        return m;
    }

}