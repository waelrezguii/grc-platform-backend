package com.project.grcplatform.service;

import com.project.grcplatform.constant.*;
import com.project.grcplatform.dto.*;
import com.project.grcplatform.exception.AppException;
import com.project.grcplatform.mapper.*;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditCampaignService {

    private final AuditCampaignRepository       campaignRepository;
    private final AuditCampaignHistoryRepository historyRepository;
    private final AuditChecklistItemRepository   checklistRepository;
    private final AuditFindingRepository         findingRepository;
    private final AuditRecommendationRepository  recommendationRepository;
    private final UserRepository                 userRepository;
    private final AuditTypeRepository            auditTypeRepository;
    private final AuditService                   auditService;
    private final NotificationService            notificationService;

    // ─── Read ─────────────────────────────────────────────────────────────────

    public Page<AuditCampaignResponseDTO> findAll(String title, String auditTypeId,
                                                  AuditCampaignStatus status, String auditorId,
                                                  boolean archived,
                                                  Pageable pageable) {
        return campaignRepository.findAllWithFilters(title, auditTypeId, status, auditorId, archived, pageable)
                .map(AuditCampaignMapper::toDTO);
    }

    public AuditCampaignResponseDTO findById(String id) {
        return AuditCampaignMapper.toDTO(getOrThrow(id));
    }

    public List<AuditCampaignHistory> getHistory(String id) {
        getOrThrow(id);
        return historyRepository.findByCampaignIdOrderByCreatedAtDesc(id);
    }

    public List<AuditChecklistItemResponseDTO> getChecklist(String campaignId) {
        getOrThrow(campaignId);
        return checklistRepository.findByCampaignIdAndDeletedFalseOrderByCreatedAtAsc(campaignId)
                .stream().map(AuditChecklistItemMapper::toDTO).toList();
    }

    public List<AuditFindingResponseDTO> getFindings(String campaignId) {
        getOrThrow(campaignId);
        return findingRepository.findByCampaignIdAndDeletedFalseOrderByCreatedAtDesc(campaignId)
                .stream().map(AuditFindingMapper::toDTO).toList();
    }

    public List<AuditRecommendationResponseDTO> getRecommendations(String campaignId) {
        getOrThrow(campaignId);
        return recommendationRepository.findByCampaignIdAndDeletedFalseOrderByCreatedAtDesc(campaignId)
                .stream().map(AuditRecommendationMapper::toDTO).toList();
    }

    public AuditReportDTO generateReport(String campaignId) {
        AuditCampaign campaign = getOrThrow(campaignId);

        List<AuditChecklistItem> items = checklistRepository
                .findByCampaignIdAndDeletedFalseOrderByCreatedAtAsc(campaignId);
        Map<String, Long> checklistCounts = checklistRepository.countByResultForCampaign(campaignId)
                .stream().collect(Collectors.toMap(r -> r[0].toString(), r -> (Long) r[1]));
        long conformantCount = checklistCounts.getOrDefault(ChecklistResult.CONFORMANT.name(), 0L);
        double conformanceRate = items.isEmpty() ? 0.0
                : Math.round((conformantCount * 100.0 / items.size()) * 10.0) / 10.0;

        List<AuditFinding> findings = findingRepository
                .findByCampaignIdAndDeletedFalseOrderByCreatedAtDesc(campaignId);
        Map<String, Long> bySeverity = findings.stream()
                .collect(Collectors.groupingBy(f -> f.getSeverity().name(), Collectors.counting()));
        Map<String, Long> byType = findings.stream()
                .collect(Collectors.groupingBy(f -> f.getFindingType().name(), Collectors.counting()));

        List<AuditRecommendation> recommendations = recommendationRepository
                .findByCampaignIdAndDeletedFalseOrderByCreatedAtDesc(campaignId);
        Map<String, Long> byRecStatus = recommendations.stream()
                .collect(Collectors.groupingBy(r -> r.getStatus().name(), Collectors.counting()));

        return AuditReportDTO.builder()
                .campaignId(campaign.getId()).title(campaign.getTitle())
                .auditType(campaign.getAuditType() != null
                        ? AuditTypeDTO.builder()
                                .id(campaign.getAuditType().getId())
                                .name(campaign.getAuditType().getName())
                                .description(campaign.getAuditType().getDescription())
                                .build()
                        : null)
                .scope(campaign.getScope())
                .status(campaign.getStatus())
                .auditor(campaign.getAuditor() != null ? UserSummaryDTO.builder()
                        .id(campaign.getAuditor().getId())
                        .firstname(campaign.getAuditor().getFirstname())
                        .lastname(campaign.getAuditor().getLastname())
                        .email(campaign.getAuditor().getEmail()).build() : null)
                .auditee(campaign.getAuditee() != null ? UserSummaryDTO.builder()
                        .id(campaign.getAuditee().getId())
                        .firstname(campaign.getAuditee().getFirstname())
                        .lastname(campaign.getAuditee().getLastname())
                        .email(campaign.getAuditee().getEmail()).build() : null)
                .actualStartDate(campaign.getActualStartDate()).actualEndDate(campaign.getActualEndDate())
                .reportGeneratedAt(LocalDateTime.now())
                .totalChecklistItems(items.size()).checklistResultCounts(checklistCounts)
                .conformanceRate(conformanceRate)
                .totalFindings(findings.size()).findingsBySeverity(bySeverity).findingsByType(byType)
                .findings(findings.stream().map(AuditFindingMapper::toDTO).toList())
                .totalRecommendations(recommendations.size()).recommendationsByStatus(byRecStatus)
                .recommendations(recommendations.stream().map(AuditRecommendationMapper::toDTO).toList())
                .build();
    }

    // ─── Write ────────────────────────────────────────────────────────────────

    @Transactional
    public AuditCampaignResponseDTO create(AuditCampaignRequestDTO request) {
        User auditor = request.getAuditorId() != null ? getUserOrThrow(request.getAuditorId()) : null;
        User auditee = request.getAuditeeId() != null ? getUserOrThrow(request.getAuditeeId()) : null;
        AuditType auditType = request.getAuditTypeId() != null
                ? auditTypeRepository.findById(request.getAuditTypeId())
                        .orElseThrow(() -> new AppException(ErrorCode.AUDIT_CAMPAIGN_NOT_FOUND))
                : null;

        AuditCampaign c = AuditCampaignMapper.toEntity(request, auditor, auditee, auditType);
        AuditCampaign saved = campaignRepository.saveAndFlush(c);
        AuditCampaign fresh = campaignRepository.findByIdAndDeletedFalse(saved.getId()).orElse(saved);
        saveHistory(fresh, "Campaign created");
        auditService.log(AuditAction.AUDIT_CAMPAIGN_CREATED, AuditEntityType.AUDIT_CAMPAIGN,
                fresh.getId(), "Audit campaign created: " + fresh.getTitle());
        return AuditCampaignMapper.toDTO(fresh);
    }

    @Transactional
    public AuditCampaignResponseDTO update(String id, AuditCampaignRequestDTO request) {
        AuditCampaign c = getOrThrow(id);
        if (c.getStatus() == AuditCampaignStatus.COMPLETED || c.getStatus() == AuditCampaignStatus.CANCELLED)
            throw new AppException(ErrorCode.AUDIT_CAMPAIGN_NOT_EDITABLE);

        User auditor = request.getAuditorId() != null ? getUserOrThrow(request.getAuditorId()) : null;
        User auditee = request.getAuditeeId() != null ? getUserOrThrow(request.getAuditeeId()) : null;
        AuditType auditType = request.getAuditTypeId() != null
                ? auditTypeRepository.findById(request.getAuditTypeId())
                        .orElseThrow(() -> new AppException(ErrorCode.AUDIT_CAMPAIGN_NOT_FOUND))
                : null;

        Map<String, Object> old = snapshot(c);
        AuditCampaignMapper.updateEntity(c, request, auditor, auditee, auditType);
        AuditCampaign saved = campaignRepository.save(c);
        saveHistory(saved, "Campaign updated");
        auditService.log(AuditAction.AUDIT_CAMPAIGN_UPDATED, AuditEntityType.AUDIT_CAMPAIGN,
                saved.getId(), "Campaign updated", old, snapshot(saved));
        return AuditCampaignMapper.toDTO(saved);
    }

    @Transactional
    public AuditCampaignResponseDTO start(String id) {
        AuditCampaign c = getOrThrow(id);
        if (c.getStatus() != AuditCampaignStatus.PLANNED)
            throw new AppException(ErrorCode.AUDIT_CAMPAIGN_INVALID_TRANSITION);
        c.setStatus(AuditCampaignStatus.IN_PROGRESS);
        c.setActualStartDate(LocalDate.now());
        AuditCampaign saved = campaignRepository.save(c);
        saveHistory(saved, "Campaign started");
        auditService.log(AuditAction.AUDIT_CAMPAIGN_STARTED, AuditEntityType.AUDIT_CAMPAIGN,
                saved.getId(), "Campaign started: " + saved.getTitle());

        if (saved.getAuditor() != null) {
            notificationService.notify(
                    saved.getAuditor().getId(),
                    NotificationType.AUDIT_CAMPAIGN_STARTED,
                    NotificationEntityType.AUDIT_CAMPAIGN, saved.getId(),
                    "Campagne d'audit démarrée",
                    "La campagne \"" + saved.getTitle() + "\" a été démarrée. Vous êtes l'auditeur principal.");
        }
        return AuditCampaignMapper.toDTO(saved);
    }

    @Transactional
    public AuditCampaignResponseDTO submit(String id) {
        AuditCampaign c = getOrThrow(id);
        if (c.getStatus() != AuditCampaignStatus.IN_PROGRESS)
            throw new AppException(ErrorCode.AUDIT_CAMPAIGN_INVALID_TRANSITION);
        c.setStatus(AuditCampaignStatus.UNDER_REVIEW);
        AuditCampaign saved = campaignRepository.save(c);
        saveHistory(saved, "Campaign submitted for review");
        auditService.log(AuditAction.AUDIT_CAMPAIGN_SUBMITTED, AuditEntityType.AUDIT_CAMPAIGN,
                saved.getId(), "Campaign submitted: " + saved.getTitle());
        return AuditCampaignMapper.toDTO(saved);
    }

    @Transactional
    public AuditCampaignResponseDTO complete(String id) {
        AuditCampaign c = getOrThrow(id);
        if (c.getStatus() != AuditCampaignStatus.UNDER_REVIEW)
            throw new AppException(ErrorCode.AUDIT_CAMPAIGN_INVALID_TRANSITION);
        c.setStatus(AuditCampaignStatus.COMPLETED);
        c.setActualEndDate(LocalDate.now());
        AuditCampaign saved = campaignRepository.save(c);
        saveHistory(saved, "Campaign completed");
        auditService.log(AuditAction.AUDIT_CAMPAIGN_COMPLETED, AuditEntityType.AUDIT_CAMPAIGN,
                saved.getId(), "Campaign completed: " + saved.getTitle());

        notificationService.notify(
                saved.getCreatedBy().getId(),
                NotificationType.AUDIT_CAMPAIGN_COMPLETED,
                NotificationEntityType.AUDIT_CAMPAIGN, saved.getId(),
                "Campagne d'audit terminée",
                "La campagne \"" + saved.getTitle() + "\" est terminée. Le rapport est disponible.");

        return AuditCampaignMapper.toDTO(saved);
    }

    @Transactional
    public AuditCampaignResponseDTO cancel(String id) {
        AuditCampaign c = getOrThrow(id);
        if (c.getStatus() == AuditCampaignStatus.COMPLETED || c.getStatus() == AuditCampaignStatus.CANCELLED)
            throw new AppException(ErrorCode.AUDIT_CAMPAIGN_INVALID_TRANSITION);
        c.setStatus(AuditCampaignStatus.CANCELLED);
        AuditCampaign saved = campaignRepository.save(c);
        saveHistory(saved, "Campaign cancelled");
        auditService.log(AuditAction.AUDIT_CAMPAIGN_CANCELLED, AuditEntityType.AUDIT_CAMPAIGN,
                saved.getId(), "Campaign cancelled: " + saved.getTitle());
        return AuditCampaignMapper.toDTO(saved);
    }

    @Transactional
    public void delete(String id) {
        AuditCampaign c = getOrThrow(id);
        c.setDeleted(true);
        campaignRepository.save(c);
        auditService.log(AuditAction.AUDIT_CAMPAIGN_DELETED, AuditEntityType.AUDIT_CAMPAIGN,
                id, "Campaign deleted: " + c.getTitle());
    }

    // ─── Checklist ────────────────────────────────────────────────────────────

    @Transactional
    public AuditChecklistItemResponseDTO addChecklistItem(String campaignId,
                                                          AuditChecklistItemRequestDTO request) {
        AuditCampaign c = getOrThrow(campaignId);
        if (c.getStatus() == AuditCampaignStatus.COMPLETED || c.getStatus() == AuditCampaignStatus.CANCELLED)
            throw new AppException(ErrorCode.AUDIT_CAMPAIGN_NOT_EDITABLE);
        AuditChecklistItem item = AuditChecklistItemMapper.toEntity(request, c);
        return AuditChecklistItemMapper.toDTO(checklistRepository.save(item));
    }

    @Transactional
    public AuditChecklistItemResponseDTO updateChecklistItem(String campaignId, String itemId,
                                                             AuditChecklistItemRequestDTO request) {
        AuditCampaign c = getOrThrow(campaignId);
        if (c.getStatus() == AuditCampaignStatus.COMPLETED || c.getStatus() == AuditCampaignStatus.CANCELLED)
            throw new AppException(ErrorCode.AUDIT_CAMPAIGN_NOT_EDITABLE);
        AuditChecklistItem item = checklistRepository.findByIdAndDeletedFalse(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.AUDIT_CHECKLIST_ITEM_NOT_FOUND));
        AuditChecklistItemMapper.updateEntity(item, request);
        return AuditChecklistItemMapper.toDTO(checklistRepository.save(item));
    }

    @Transactional
    public void deleteChecklistItem(String campaignId, String itemId) {
        getOrThrow(campaignId);
        AuditChecklistItem item = checklistRepository.findByIdAndDeletedFalse(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.AUDIT_CHECKLIST_ITEM_NOT_FOUND));
        item.setDeleted(true);
        checklistRepository.save(item);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private AuditCampaign getOrThrow(String id) {
        return campaignRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.AUDIT_CAMPAIGN_NOT_FOUND));
    }

    private User getUserOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private void saveHistory(AuditCampaign c, String summary) {
        historyRepository.save(AuditCampaignHistory.builder()
                .campaignId(c.getId())
                .changedBy(getCurrentUserId())
                .changeSummary(summary)
                .snapshot(snapshot(c))
                .build());
    }

    private Map<String, Object> snapshot(AuditCampaign c) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("title", c.getTitle());
        m.put("description", c.getDescription());
        m.put("auditType", c.getAuditType() != null ? c.getAuditType().getName() : null);
        m.put("scope", c.getScope());
        m.put("status", c.getStatus() != null ? c.getStatus().name() : null);
        m.put("auditorId", c.getAuditor() != null ? c.getAuditor().getId() : null);
        m.put("auditeeId", c.getAuditee() != null ? c.getAuditee().getId() : null);
        m.put("createdById", c.getCreatedBy() != null ? c.getCreatedBy().getId() : null);
        m.put("plannedStartDate", c.getPlannedStartDate() != null ? c.getPlannedStartDate().toString() : null);
        m.put("plannedEndDate", c.getPlannedEndDate() != null ? c.getPlannedEndDate().toString() : null);
        m.put("actualStartDate", c.getActualStartDate() != null ? c.getActualStartDate().toString() : null);
        m.put("actualEndDate", c.getActualEndDate() != null ? c.getActualEndDate().toString() : null);
        return m;
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthToken j) return j.getUserId();
        return "system";
    }
}