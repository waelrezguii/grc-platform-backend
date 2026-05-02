package com.project.grcplatform.service;

import com.project.grcplatform.constant.*;
import com.project.grcplatform.dto.GovernanceResponsibilityRequestDTO;
import com.project.grcplatform.dto.GovernanceResponsibilityResponseDTO;
import com.project.grcplatform.exception.AppException;
import com.project.grcplatform.exception.NotFoundException;
import com.project.grcplatform.mapper.GovernanceResponsibilityMapper;
import com.project.grcplatform.model.GovernanceResponsibility;
import com.project.grcplatform.model.GovernanceResponsibilityHistory;
import com.project.grcplatform.model.ResponsibilityType;
import com.project.grcplatform.repository.GovernanceResponsibilityHistoryRepository;
import com.project.grcplatform.repository.GovernanceResponsibilityRepository;
import com.project.grcplatform.repository.ResponsibilityTypeRepository;
import com.project.grcplatform.repository.UserRepository;
import com.project.grcplatform.security.JwtAuthToken;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GovernanceResponsibilityService {

    private final GovernanceResponsibilityRepository responsibilityRepository;
    private final GovernanceResponsibilityHistoryRepository historyRepository;
    private final ResponsibilityTypeRepository responsibilityTypeRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    // ─── Read ────────────────────────────────────────────────────────────────────

    public Page<GovernanceResponsibilityResponseDTO> findAll(String title,
                                                             String responsibilityTypeId,
                                                             ResponsibilityStatus status,
                                                             String assigneeId,
                                                             Pageable pageable) {
        return responsibilityRepository
                .findAllWithFilters(title, responsibilityTypeId, status, assigneeId, pageable)
                .map(GovernanceResponsibilityMapper::toDTO);
    }

    public GovernanceResponsibilityResponseDTO findById(String id) {
        return GovernanceResponsibilityMapper.toDTO(getOrThrow(id));
    }

    public List<GovernanceResponsibilityHistory> getHistory(String id) {
        getOrThrow(id);
        return historyRepository.findByResponsibility_IdOrderByCreatedAtDesc(id);
    }

    // ─── Write ───────────────────────────────────────────────────────────────────

    @Transactional
    public GovernanceResponsibilityResponseDTO create(GovernanceResponsibilityRequestDTO request) {
        GovernanceResponsibility r = GovernanceResponsibilityMapper.toEntity(request);
        String currentUserId = getCurrentUserId();
        userRepository.findById(request.getAssigneeId()).ifPresent(r::setAssignee);
        userRepository.findById(currentUserId).ifPresent(r::setOwner);

        if (request.getResponsibilityTypeId() != null) {
            ResponsibilityType responsibilityType = responsibilityTypeRepository.findById(request.getResponsibilityTypeId())
                    .orElseThrow(() -> new NotFoundException("Responsibility type not found: " + request.getResponsibilityTypeId()));
            r.setResponsibilityType(responsibilityType);
        }

        GovernanceResponsibility saved = responsibilityRepository.saveAndFlush(r);
        GovernanceResponsibility fresh = responsibilityRepository.findByIdAndDeletedFalse(saved.getId()).orElse(saved);
        saveHistory(fresh, "Responsibility created");

        auditService.log(AuditAction.GOVERNANCE_RESPONSIBILITY_CREATED, AuditEntityType.GOVERNANCE_RESPONSIBILITY,
                fresh.getId(), "Responsibility created: " + fresh.getTitle());

        // Notify the assignee
        if (fresh.getAssignee() != null) {
            notificationService.notify(
                    fresh.getAssignee().getId(),
                    NotificationType.RESPONSIBILITY_ASSIGNED,
                    NotificationEntityType.GOVERNANCE_RESPONSIBILITY,
                    fresh.getId(),
                    "Responsabilité assignée",
                    "La responsabilité \"" + fresh.getTitle() + "\" vous a été assignée."
            );
        }

        return GovernanceResponsibilityMapper.toDTO(fresh);
    }

    @Transactional
    public GovernanceResponsibilityResponseDTO update(String id, GovernanceResponsibilityRequestDTO request) {
        GovernanceResponsibility r = getOrThrow(id);
        if (r.getStatus() == ResponsibilityStatus.TRANSFERRED) {
            throw new AppException(ErrorCode.GOVERNANCE_RESPONSIBILITY_INVALID_TRANSITION);
        }
        Map<String, Object> old = snapshot(r);
        String previousAssignee = r.getAssignee() != null ? r.getAssignee().getId() : null;

        GovernanceResponsibilityMapper.updateEntity(r, request);
        if (request.getAssigneeId() != null) {
            userRepository.findById(request.getAssigneeId()).ifPresent(r::setAssignee);
        }

        if (request.getResponsibilityTypeId() != null) {
            ResponsibilityType responsibilityType = responsibilityTypeRepository.findById(request.getResponsibilityTypeId())
                    .orElseThrow(() -> new NotFoundException("Responsibility type not found: " + request.getResponsibilityTypeId()));
            r.setResponsibilityType(responsibilityType);
        }

        GovernanceResponsibility saved = responsibilityRepository.save(r);
        saveHistory(saved, "Responsibility updated");

        auditService.log(AuditAction.GOVERNANCE_RESPONSIBILITY_UPDATED, AuditEntityType.GOVERNANCE_RESPONSIBILITY,
                saved.getId(), "Responsibility updated: " + saved.getTitle(), old, snapshot(saved));

        // If assignee changed, notify new assignee
        if (request.getAssigneeId() != null && !request.getAssigneeId().equals(previousAssignee) && saved.getAssignee() != null) {
            notificationService.notify(
                    saved.getAssignee().getId(),
                    NotificationType.RESPONSIBILITY_ASSIGNED,
                    NotificationEntityType.GOVERNANCE_RESPONSIBILITY,
                    saved.getId(),
                    "Responsabilité assignée",
                    "La responsabilité \"" + saved.getTitle() + "\" vous a été assignée."
            );
        }

        return GovernanceResponsibilityMapper.toDTO(saved);
    }

    @Transactional
    public GovernanceResponsibilityResponseDTO updateStatus(String id, ResponsibilityStatus newStatus) {
        GovernanceResponsibility r = getOrThrow(id);
        if (r.getStatus() == ResponsibilityStatus.TRANSFERRED) {
            throw new AppException(ErrorCode.GOVERNANCE_RESPONSIBILITY_INVALID_TRANSITION);
        }
        r.setStatus(newStatus);
        GovernanceResponsibility saved = responsibilityRepository.save(r);
        saveHistory(saved, "Responsibility status changed to " + newStatus);

        auditService.log(AuditAction.GOVERNANCE_RESPONSIBILITY_UPDATED, AuditEntityType.GOVERNANCE_RESPONSIBILITY,
                saved.getId(), "Status changed to " + newStatus);

        return GovernanceResponsibilityMapper.toDTO(saved);
    }

    @Transactional
    public void delete(String id) {
        GovernanceResponsibility r = getOrThrow(id);
        r.setDeleted(true);
        responsibilityRepository.save(r);
        saveHistory(r, "Responsibility deleted");

        auditService.log(AuditAction.GOVERNANCE_RESPONSIBILITY_DELETED, AuditEntityType.GOVERNANCE_RESPONSIBILITY,
                id, "Responsibility deleted: " + r.getTitle());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private GovernanceResponsibility getOrThrow(String id) {
        return responsibilityRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.GOVERNANCE_RESPONSIBILITY_NOT_FOUND));
    }

    private void saveHistory(GovernanceResponsibility r, String summary) {
        GovernanceResponsibilityHistory h = GovernanceResponsibilityHistory.builder()
                .responsibility(r)
                .changeSummary(summary)
                .snapshot(snapshot(r))
                .build();
        historyRepository.save(h);
    }

    private Map<String, Object> snapshot(GovernanceResponsibility r) {
        Map<String, Object> map = new HashMap<>();
        map.put("id",                 r.getId());
        map.put("title",              r.getTitle());
        map.put("description",        r.getDescription());
        map.put("responsibilityType", r.getResponsibilityType() != null ? r.getResponsibilityType().getName() : null);
        map.put("assigneeId",         r.getAssignee() != null ? r.getAssignee().getId() : null);
        map.put("scope",              r.getScope());
        map.put("startDate",          r.getStartDate() != null ? r.getStartDate().toString() : null);
        map.put("endDate",            r.getEndDate()   != null ? r.getEndDate().toString()   : null);
        map.put("status",             r.getStatus()    != null ? r.getStatus().name()        : null);
        map.put("ownerId",            r.getOwner() != null ? r.getOwner().getId() : null);
        return map;
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthToken jwtAuthToken) return jwtAuthToken.getUserId();
        return "system";
    }
}
