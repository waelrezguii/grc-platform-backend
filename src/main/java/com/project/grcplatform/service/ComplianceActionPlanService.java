package com.project.grcplatform.service;

import com.project.grcplatform.constant.*;
import com.project.grcplatform.dto.*;
import com.project.grcplatform.exception.AppException;

import com.project.grcplatform.mapper.ComplianceActionPlanMapper;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComplianceActionPlanService {

    private final ComplianceActionPlanRepository planRepository;
    private final ComplianceActionPlanHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final ComplianceEvaluationRepository evaluationRepository;
    private final ComplianceRequirementRepository requirementRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public Page<ComplianceActionPlanResponseDTO> findAll(String title, String evaluationId,
                                                         ActionPlanStatus status, String assigneeId,
                                                         ActionPlanPriority priority, Pageable pageable) {
        return planRepository.findAllWithFilters(title, evaluationId, status, assigneeId, priority, pageable)
                .map(ComplianceActionPlanMapper::toDTO);
    }

    public ComplianceActionPlanResponseDTO findById(String id) {
        return ComplianceActionPlanMapper.toDTO(getOrThrow(id));
    }

    public List<ComplianceActionPlanHistory> getHistory(String id) {
        getOrThrow(id);
        return historyRepository.findByActionPlan_IdOrderByCreatedAtDesc(id);
    }

    @Transactional
    public ComplianceActionPlanResponseDTO create(ComplianceActionPlanRequestDTO request) {
        ComplianceActionPlan plan = ComplianceActionPlanMapper.toEntity(request);
        evaluationRepository.findById(request.getEvaluationId()).ifPresent(plan::setEvaluation);
        if (request.getRequirementId() != null) requirementRepository.findById(request.getRequirementId()).ifPresent(plan::setRequirement);
        if (request.getAssigneeId() != null) userRepository.findById(request.getAssigneeId()).ifPresent(plan::setAssignee);
        String userId = getCurrentUserId();
        userRepository.findById(userId).ifPresent(plan::setOwner);
        ComplianceActionPlan saved = planRepository.saveAndFlush(plan);
        ComplianceActionPlan fresh = planRepository.findByIdAndDeletedFalse(saved.getId()).orElse(saved);
        saveHistory(fresh, "Action plan created");

        auditService.log(AuditAction.COMPLIANCE_ACTION_PLAN_CREATED, AuditEntityType.COMPLIANCE_ACTION_PLAN,
                fresh.getId(), "Action plan created: " + fresh.getTitle());

        // Notify assignee
        if (fresh.getAssignee() != null) {
            notificationService.notify(
                    fresh.getAssignee().getId(),
                    NotificationType.COMPLIANCE_ACTION_ASSIGNED,
                    NotificationEntityType.COMPLIANCE_ACTION_PLAN,
                    fresh.getId(),
                    "Plan d'action de conformité assigné",
                    "Le plan d'action \"" + fresh.getTitle() + "\" vous a été assigné."
                            + (fresh.getDueDate() != null ? " Échéance : " + fresh.getDueDate() + "." : "")
            );
        }

        return ComplianceActionPlanMapper.toDTO(fresh);
    }

    @Transactional
    public ComplianceActionPlanResponseDTO update(String id, ComplianceActionPlanRequestDTO request) {
        ComplianceActionPlan plan = getOrThrow(id);
        if (plan.getStatus() == ActionPlanStatus.DONE || plan.getStatus() == ActionPlanStatus.CANCELLED)
            throw new AppException(ErrorCode.COMPLIANCE_ACTION_PLAN_INVALID_TRANSITION);

        Map<String, Object> old = snapshot(plan);
        String previousAssignee = plan.getAssignee() != null ? plan.getAssignee().getId() : null;
        ComplianceActionPlanMapper.updateEntity(plan, request);
        if (request.getAssigneeId() != null) userRepository.findById(request.getAssigneeId()).ifPresent(plan::setAssignee);
        ComplianceActionPlan saved = planRepository.save(plan);
        saveHistory(saved, "Action plan updated");

        auditService.log(AuditAction.COMPLIANCE_ACTION_PLAN_UPDATED, AuditEntityType.COMPLIANCE_ACTION_PLAN,
                saved.getId(), "Action plan updated: " + saved.getTitle(), old, snapshot(saved));

        // Notify new assignee if changed
        if (request.getAssigneeId() != null && !request.getAssigneeId().equals(previousAssignee)) {
            notificationService.notify(
                    saved.getAssignee() != null ? saved.getAssignee().getId() : null,
                    NotificationType.COMPLIANCE_ACTION_ASSIGNED,
                    NotificationEntityType.COMPLIANCE_ACTION_PLAN,
                    saved.getId(),
                    "Plan d'action de conformité assigné",
                    "Le plan d'action \"" + saved.getTitle() + "\" vous a été assigné."
            );
        }

        return ComplianceActionPlanMapper.toDTO(saved);
    }

    @Transactional
    public ComplianceActionPlanResponseDTO updateStatus(String id, ActionPlanStatus newStatus) {
        ComplianceActionPlan plan = getOrThrow(id);
        if (plan.getStatus() == ActionPlanStatus.DONE || plan.getStatus() == ActionPlanStatus.CANCELLED)
            throw new AppException(ErrorCode.COMPLIANCE_ACTION_PLAN_INVALID_TRANSITION);

        plan.setStatus(newStatus);
        if (newStatus == ActionPlanStatus.DONE) {
            plan.setCompletedAt(LocalDateTime.now());
        }
        ComplianceActionPlan saved = planRepository.save(plan);
        saveHistory(saved, "Status changed to " + newStatus);

        auditService.log(AuditAction.COMPLIANCE_ACTION_PLAN_UPDATED, AuditEntityType.COMPLIANCE_ACTION_PLAN,
                saved.getId(), "Status changed to " + newStatus);

        return ComplianceActionPlanMapper.toDTO(saved);
    }

    @Transactional
    public void delete(String id) {
        ComplianceActionPlan plan = getOrThrow(id);
        plan.setDeleted(true);
        planRepository.save(plan);
        auditService.log(AuditAction.COMPLIANCE_ACTION_PLAN_DELETED, AuditEntityType.COMPLIANCE_ACTION_PLAN,
                id, "Action plan deleted: " + plan.getTitle());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private ComplianceActionPlan getOrThrow(String id) {
        return planRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.COMPLIANCE_ACTION_PLAN_NOT_FOUND));
    }

    private void saveHistory(ComplianceActionPlan p, String summary) {
        historyRepository.save(ComplianceActionPlanHistory.builder()
                .actionPlan(p)
                .changeSummary(summary).snapshot(snapshot(p)).build());
    }

    private Map<String, Object> snapshot(ComplianceActionPlan p) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId()); m.put("evaluationId", p.getEvaluation() != null ? p.getEvaluation().getId() : null);
        m.put("requirementId", p.getRequirement() != null ? p.getRequirement().getId() : null); m.put("title", p.getTitle());
        m.put("description", p.getDescription());
        m.put("priority", p.getPriority() != null ? p.getPriority().name() : null);
        m.put("status", p.getStatus() != null ? p.getStatus().name() : null);
        m.put("assigneeId", p.getAssignee() != null ? p.getAssignee().getId() : null);
        m.put("dueDate", p.getDueDate() != null ? p.getDueDate().toString() : null);
        m.put("completedAt", p.getCompletedAt() != null ? p.getCompletedAt().toString() : null);
        m.put("ownerId", p.getOwner() != null ? p.getOwner().getId() : null);
        return m;
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthToken j) return j.getUserId();
        return "system";
    }
}