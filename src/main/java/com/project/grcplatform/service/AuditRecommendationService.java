package com.project.grcplatform.service;

import com.project.grcplatform.constant.*;
import com.project.grcplatform.dto.*;
import com.project.grcplatform.exception.AppException;
import com.project.grcplatform.mapper.AuditRecommendationMapper;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditRecommendationService {

    private final AuditRecommendationRepository         recommendationRepository;
    private final AuditRecommendationHistoryRepository   historyRepository;
    private final AuditCampaignRepository               campaignRepository;
    private final AuditFindingRepository                findingRepository;
    private final UserRepository                        userRepository;
    private final AuditService                          auditService;
    private final NotificationService                   notificationService;

    public Page<AuditRecommendationResponseDTO> findAll(String campaignId, String findingId,
                                                        RecommendationStatus status, String assigneeId,
                                                        RecommendationPriority priority,
                                                        Pageable pageable) {
        return recommendationRepository
                .findAllWithFilters(campaignId, findingId, status, assigneeId, priority, pageable)
                .map(AuditRecommendationMapper::toDTO);
    }

    public AuditRecommendationResponseDTO findById(String id) {
        return AuditRecommendationMapper.toDTO(getOrThrow(id));
    }

    public List<AuditRecommendationHistory> getHistory(String id) {
        getOrThrow(id);
        return historyRepository.findByRecommendationIdOrderByCreatedAtDesc(id);
    }

    @Transactional
    public AuditRecommendationResponseDTO create(AuditRecommendationRequestDTO request) {
        AuditCampaign campaign = campaignRepository.findByIdAndDeletedFalse(request.getCampaignId())
                .orElseThrow(() -> new AppException(ErrorCode.AUDIT_CAMPAIGN_NOT_FOUND));

        AuditFinding finding = null;
        if (request.getFindingId() != null) {
            finding = findingRepository.findByIdAndDeletedFalse(request.getFindingId())
                    .orElseThrow(() -> new AppException(ErrorCode.AUDIT_FINDING_NOT_FOUND));
        }

        User owner    = getUserOrThrow(getCurrentUserId());
        User assignee = request.getAssigneeId() != null ? getUserOrThrow(request.getAssigneeId()) : null;

        AuditRecommendation r = AuditRecommendationMapper.toEntity(request, campaign, finding, assignee, owner);
        AuditRecommendation saved = recommendationRepository.saveAndFlush(r);
        AuditRecommendation fresh = recommendationRepository.findByIdAndDeletedFalse(saved.getId()).orElse(saved);
        saveHistory(fresh, "Recommendation created");
        auditService.log(AuditAction.AUDIT_RECOMMENDATION_CREATED, AuditEntityType.AUDIT_RECOMMENDATION,
                fresh.getId(), "Recommendation created: " + fresh.getTitle());

        // Notify assignee
        if (fresh.getAssignee() != null) {
            notificationService.notify(
                    fresh.getAssignee().getId(),
                    NotificationType.AUDIT_RECOMMENDATION_ASSIGNED,
                    NotificationEntityType.AUDIT_RECOMMENDATION, fresh.getId(),
                    "Recommandation d'audit assignée",
                    "Une recommandation vous a été assignée : \"" + fresh.getTitle() + "\"");
        }
        return AuditRecommendationMapper.toDTO(fresh);
    }

    @Transactional
    public AuditRecommendationResponseDTO update(String id, AuditRecommendationRequestDTO request) {
        AuditRecommendation r = getOrThrow(id);
        if (r.getStatus() == RecommendationStatus.IMPLEMENTED
                || r.getStatus() == RecommendationStatus.REJECTED
                || r.getStatus() == RecommendationStatus.CLOSED)
            throw new AppException(ErrorCode.AUDIT_RECOMMENDATION_INVALID_TRANSITION);

        AuditFinding finding = null;
        if (request.getFindingId() != null) {
            finding = findingRepository.findByIdAndDeletedFalse(request.getFindingId())
                    .orElseThrow(() -> new AppException(ErrorCode.AUDIT_FINDING_NOT_FOUND));
        }

        User assignee = request.getAssigneeId() != null ? getUserOrThrow(request.getAssigneeId()) : null;
        String previousAssigneeId = r.getAssignee() != null ? r.getAssignee().getId() : null;

        Map<String, Object> old = snapshot(r);
        AuditRecommendationMapper.updateEntity(r, request, finding, assignee);
        AuditRecommendation saved = recommendationRepository.save(r);
        saveHistory(saved, "Recommendation updated");
        auditService.log(AuditAction.AUDIT_RECOMMENDATION_UPDATED, AuditEntityType.AUDIT_RECOMMENDATION,
                saved.getId(), "Recommendation updated", old, snapshot(saved));

        // Notify new assignee if changed
        String newAssigneeId = saved.getAssignee() != null ? saved.getAssignee().getId() : null;
        if (newAssigneeId != null && !Objects.equals(previousAssigneeId, newAssigneeId)) {
            notificationService.notify(
                    newAssigneeId,
                    NotificationType.AUDIT_RECOMMENDATION_ASSIGNED,
                    NotificationEntityType.AUDIT_RECOMMENDATION, saved.getId(),
                    "Recommandation d'audit assignée",
                    "Une recommandation vous a été assignée : \"" + saved.getTitle() + "\"");
        }
        return AuditRecommendationMapper.toDTO(saved);
    }

    @Transactional
    public AuditRecommendationResponseDTO updateStatus(String id, RecommendationStatus newStatus) {
        AuditRecommendation r = getOrThrow(id);
        validateTransition(r.getStatus(), newStatus);
        Map<String, Object> old = snapshot(r);
        r.setStatus(newStatus);
        if (newStatus == RecommendationStatus.IMPLEMENTED) {
            r.setImplementedAt(LocalDateTime.now());
        }
        AuditRecommendation saved = recommendationRepository.save(r);
        saveHistory(saved, "Status changed to " + newStatus);
        auditService.log(AuditAction.AUDIT_RECOMMENDATION_STATUS_CHANGED, AuditEntityType.AUDIT_RECOMMENDATION,
                saved.getId(), "Status: " + old.get("status") + " → " + newStatus, old, snapshot(saved));
        return AuditRecommendationMapper.toDTO(saved);
    }

    @Transactional
    public void delete(String id) {
        AuditRecommendation r = getOrThrow(id);
        r.setDeleted(true);
        recommendationRepository.save(r);
        auditService.log(AuditAction.AUDIT_RECOMMENDATION_DELETED, AuditEntityType.AUDIT_RECOMMENDATION,
                id, "Recommendation deleted: " + r.getTitle());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void validateTransition(RecommendationStatus current, RecommendationStatus next) {
        boolean valid = switch (current) {
            case PENDING     -> next == RecommendationStatus.IN_PROGRESS || next == RecommendationStatus.REJECTED;
            case IN_PROGRESS -> next == RecommendationStatus.IMPLEMENTED || next == RecommendationStatus.REJECTED;
            case IMPLEMENTED -> next == RecommendationStatus.CLOSED;
            case REJECTED    -> next == RecommendationStatus.CLOSED;
            case CLOSED      -> false;
        };
        if (!valid) throw new AppException(ErrorCode.AUDIT_RECOMMENDATION_INVALID_TRANSITION);
    }

    private AuditRecommendation getOrThrow(String id) {
        return recommendationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.AUDIT_RECOMMENDATION_NOT_FOUND));
    }

    private User getUserOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private void saveHistory(AuditRecommendation r, String summary) {
        historyRepository.save(AuditRecommendationHistory.builder()
                .recommendationId(r.getId())
                .changeSummary(summary)
                .snapshot(snapshot(r))
                .build());
    }

    private Map<String, Object> snapshot(AuditRecommendation r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("campaignId", r.getCampaign() != null ? r.getCampaign().getId() : null);
        m.put("findingId", r.getFinding() != null ? r.getFinding().getId() : null);
        m.put("title", r.getTitle());
        m.put("description", r.getDescription());
        m.put("priority", r.getPriority() != null ? r.getPriority().name() : null);
        m.put("status", r.getStatus() != null ? r.getStatus().name() : null);
        m.put("assigneeId", r.getAssignee() != null ? r.getAssignee().getId() : null);
        m.put("ownerId", r.getOwner() != null ? r.getOwner().getId() : null);
        m.put("dueDate", r.getDueDate() != null ? r.getDueDate().toString() : null);
        m.put("implementedAt", r.getImplementedAt() != null ? r.getImplementedAt().toString() : null);
        return m;
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthToken j) return j.getUserId();
        return "system";
    }
}