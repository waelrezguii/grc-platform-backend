package com.project.grcplatform.service;

import com.project.grcplatform.constant.*;
import com.project.grcplatform.dto.*;
import com.project.grcplatform.exception.AppException;
import com.project.grcplatform.mapper.TreatmentPlanMapper;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TreatmentPlanService {

    private final TreatmentPlanRepository planRepository;
    private final TreatmentActionRepository actionRepository;
    private final TreatmentPlanHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final RiskScenarioRepository scenarioRepository;
    private final RiskAssessmentRepository assessmentRepository;

    // Residual/Target risk matrix [riskScore-1][effectiveness-1]
    private static final short[][] TARGET_MATRIX = {
        {1, 1, 1},  // Mineur
        {1, 2, 2},  // Notable
        {1, 2, 3},  // Fort
        {2, 3, 4},  // Très fort
        {3, 4, 5},  // Critique
    };
    public Page<TreatmentPlanResponseDTO> findAll(String title,
                                                  String scenarioId,
                                                  String assessmentId,
                                                  String ownerId,
                                                  TreatmentPlanStatus status,
                                                  TreatmentStrategy treatmentStrategy,
                                                  boolean archived,
                                                  Pageable pageable) {
        return planRepository
                .findAllWithFilters(title, scenarioId, assessmentId, ownerId, status, treatmentStrategy, archived, pageable)
                .map(TreatmentPlanMapper::toDTO);
    }

    public TreatmentPlanResponseDTO findById(String id) {
        return TreatmentPlanMapper.toDTO(getOrThrow(id));
    }

    @Transactional
    public TreatmentPlanResponseDTO create(TreatmentPlanRequestDTO request) {
        TreatmentPlan plan = TreatmentPlanMapper.toEntity(request);
        // ── FIX : set scenario from scenarioId ──────────────────
        RiskScenario scenario = scenarioRepository.findByIdAndDeletedFalse(request.getScenarioId())
                .orElseThrow(() -> new AppException(ErrorCode.SCENARIO_NOT_FOUND));
        plan.setScenario(scenario);

        // ── set assessment if provided ───────────────────────────
        if (request.getAssessmentId() != null) {
            assessmentRepository.findByIdAndDeletedFalse(request.getAssessmentId())
                    .ifPresent(plan::setAssessment);
        }
        computeTargetRisk(plan);
        TreatmentPlan saved = planRepository.saveAndFlush(plan);
        TreatmentPlan fresh = planRepository.findByIdAndDeletedFalse(saved.getId()).orElse(saved);
        saveHistory(fresh, "Treatment plan created");
        auditService.log(
                AuditAction.TREATMENT_PLAN_CREATED,
                AuditEntityType.TREATMENT_PLAN,
                fresh.getId(),
                "Treatment plan created: " + fresh.getTitle()
        );
        return TreatmentPlanMapper.toDTO(fresh);
    }

    @Transactional
    public TreatmentPlanResponseDTO update(String id, TreatmentPlanRequestDTO request) {
        TreatmentPlan plan = getOrThrow(id);
        if (plan.getStatus() == TreatmentPlanStatus.COMPLETED ||
                plan.getStatus() == TreatmentPlanStatus.CANCELLED) {
            throw new AppException(ErrorCode.TREATMENT_PLAN_NOT_EDITABLE);
        }
        Map<String, Object> oldValues = Map.of(
                "title",   plan.getTitle()   != null ? plan.getTitle()              : "",
                "status",  plan.getStatus()  != null ? plan.getStatus().name()      : "",
                "dueDate", plan.getDueDate() != null ? plan.getDueDate().toString() : ""
        );
        TreatmentPlanMapper.updateEntity(plan, request);
        computeTargetRisk(plan);
        TreatmentPlan saved = planRepository.save(plan);
        saveHistory(saved, "Treatment plan updated");
        auditService.log(
                AuditAction.TREATMENT_PLAN_UPDATED,
                AuditEntityType.TREATMENT_PLAN,
                saved.getId(),
                "Treatment plan updated: " + saved.getTitle(),
                oldValues,
                Map.of(
                        "title",   saved.getTitle()   != null ? saved.getTitle()              : "",
                        "status",  saved.getStatus()  != null ? saved.getStatus().name()      : "",
                        "dueDate", saved.getDueDate() != null ? saved.getDueDate().toString() : ""
                )
        );
        return TreatmentPlanMapper.toDTO(saved);
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────────

    @Transactional
    public TreatmentPlanResponseDTO approve(String id) {
        TreatmentPlan plan = getOrThrow(id);
        if (plan.getStatus() != TreatmentPlanStatus.DRAFT) {
            throw new AppException(ErrorCode.TREATMENT_PLAN_INVALID_TRANSITION);
        }
        plan.setStatus(TreatmentPlanStatus.APPROVED);
        TreatmentPlan saved = planRepository.save(plan);
        saveHistory(saved, "Treatment plan approved");
        auditService.log(
                AuditAction.TREATMENT_PLAN_APPROVED,
                AuditEntityType.TREATMENT_PLAN,
                saved.getId(),
                "Treatment plan approved: " + saved.getTitle()
        );

        // Notify the creator that the plan has been approved
        if (saved.getCreatedBy() != null) {
            notificationService.notify(
                    saved.getCreatedBy().getId(),
                    NotificationType.TREATMENT_PLAN_APPROVED,
                    NotificationEntityType.TREATMENT_PLAN,
                    saved.getId(),
                    "Treatment Plan Approved",
                    "Your treatment plan \"" + saved.getTitle() + "\" has been approved and is ready to start."
            );
        }

        return TreatmentPlanMapper.toDTO(saved);
    }

    @Transactional
    public TreatmentPlanResponseDTO startProgress(String id) {
        TreatmentPlan plan = getOrThrow(id);
        if (plan.getStatus() != TreatmentPlanStatus.APPROVED) {
            throw new AppException(ErrorCode.TREATMENT_PLAN_INVALID_TRANSITION);
        }
        plan.setStatus(TreatmentPlanStatus.IN_PROGRESS);
        TreatmentPlan saved = planRepository.save(plan);
        saveHistory(saved, "Treatment plan started");
        auditService.log(
                AuditAction.TREATMENT_PLAN_STARTED,
                AuditEntityType.TREATMENT_PLAN,
                saved.getId(),
                "Treatment plan started: " + saved.getTitle()
        );
        return TreatmentPlanMapper.toDTO(saved);
    }

    @Transactional
    public TreatmentPlanResponseDTO complete(String id) {
        TreatmentPlan plan = getOrThrow(id);
        if (plan.getStatus() != TreatmentPlanStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.TREATMENT_PLAN_INVALID_TRANSITION);
        }
        plan.setStatus(TreatmentPlanStatus.COMPLETED);
        TreatmentPlan saved = planRepository.save(plan);
        saveHistory(saved, "Treatment plan completed");
        auditService.log(
                AuditAction.TREATMENT_PLAN_COMPLETED,
                AuditEntityType.TREATMENT_PLAN,
                saved.getId(),
                "Treatment plan completed: " + saved.getTitle()
        );
        return TreatmentPlanMapper.toDTO(saved);
    }

    @Transactional
    public TreatmentPlanResponseDTO cancel(String id) {
        TreatmentPlan plan = getOrThrow(id);
        if (plan.getStatus() == TreatmentPlanStatus.COMPLETED ||
                plan.getStatus() == TreatmentPlanStatus.CANCELLED) {
            throw new AppException(ErrorCode.TREATMENT_PLAN_INVALID_TRANSITION);
        }
        plan.setStatus(TreatmentPlanStatus.CANCELLED);
        TreatmentPlan saved = planRepository.save(plan);
        saveHistory(saved, "Treatment plan cancelled");
        auditService.log(
                AuditAction.TREATMENT_PLAN_CANCELLED,
                AuditEntityType.TREATMENT_PLAN,
                saved.getId(),
                "Treatment plan cancelled: " + saved.getTitle()
        );
        return TreatmentPlanMapper.toDTO(saved);
    }

    // ─── Actions ─────────────────────────────────────────────────────────────────

    @Transactional
    public TreatmentActionResponseDTO addAction(String planId, TreatmentActionRequestDTO request) {
        TreatmentPlan plan = getOrThrow(planId);
        if (plan.getStatus() == TreatmentPlanStatus.COMPLETED ||
                plan.getStatus() == TreatmentPlanStatus.CANCELLED) {
            throw new AppException(ErrorCode.TREATMENT_PLAN_NOT_EDITABLE);
        }
        TreatmentAction action = TreatmentPlanMapper.toActionEntity(request, planId);
        action.setPlan(plan);
        String currentUserId = getCurrentUserId();
        if (action.getAssignee() == null) userRepository.findById(currentUserId).ifPresent(action::setAssignee);
        TreatmentAction saved = actionRepository.save(action);
        recalcProgress(plan);
        auditService.log(
                AuditAction.ACTION_CREATED,
                AuditEntityType.ACTION,
                saved.getId(),
                "Action added to plan " + planId + ": " + saved.getTitle()
        );

        // Notify assignee
        if (saved.getAssignee() != null) {
            notificationService.notify(
                    saved.getAssignee().getId(),
                    NotificationType.ACTION_ASSIGNED,
                    NotificationEntityType.ACTION,
                    saved.getId(),
                    "Action Assigned to You",
                    "You have been assigned action \"" + saved.getTitle() + "\" in treatment plan \""
                            + plan.getTitle() + "\"."
                            + (saved.getDueDate() != null ? " Due: " + saved.getDueDate() + "." : "")
            );
        }

        return TreatmentPlanMapper.toActionDTO(saved);
    }

    @Transactional
    public TreatmentActionResponseDTO updateAction(String planId, String actionId,
                                                   TreatmentActionRequestDTO request) {
        getOrThrow(planId);
        TreatmentAction action = getActionOrThrow(actionId);
        TreatmentPlanMapper.updateActionEntity(action, request);
        TreatmentAction saved = actionRepository.save(action);
        recalcProgress(getOrThrow(planId));
        auditService.log(
                AuditAction.ACTION_UPDATED,
                AuditEntityType.ACTION,
                saved.getId(),
                "Action updated: " + saved.getTitle()
        );
        return TreatmentPlanMapper.toActionDTO(saved);
    }

    @Transactional
    public TreatmentActionResponseDTO completeAction(String planId, String actionId) {
        TreatmentPlan plan = getOrThrow(planId);
        TreatmentAction action = getActionOrThrow(actionId);
        if (action.getStatus() == ActionStatus.DONE) {
            throw new AppException(ErrorCode.ACTION_ALREADY_COMPLETED);
        }
        action.setStatus(ActionStatus.DONE);
        TreatmentAction saved = actionRepository.save(action);
        recalcProgress(plan);
        auditService.log(
                AuditAction.ACTION_COMPLETED,
                AuditEntityType.ACTION,
                saved.getId(),
                "Action completed: " + saved.getTitle()
        );
        return TreatmentPlanMapper.toActionDTO(saved);
    }

    @Transactional
    public TreatmentActionResponseDTO cancelAction(String planId, String actionId) {
        TreatmentPlan plan = getOrThrow(planId);
        TreatmentAction action = getActionOrThrow(actionId);
        action.setStatus(ActionStatus.CANCELLED);
        TreatmentAction saved = actionRepository.save(action);
        recalcProgress(plan);
        auditService.log(
                AuditAction.ACTION_CANCELLED,
                AuditEntityType.ACTION,
                saved.getId(),
                "Action cancelled: " + saved.getTitle()
        );
        return TreatmentPlanMapper.toActionDTO(saved);
    }

    @Transactional
    public void deleteAction(String planId, String actionId) {
        TreatmentPlan plan = getOrThrow(planId);
        TreatmentAction action = getActionOrThrow(actionId);
        action.setDeleted(true);
        actionRepository.save(action);
        recalcProgress(plan);
        auditService.log(
                AuditAction.ACTION_DELETED,
                AuditEntityType.ACTION,
                actionId,
                "Action deleted: " + action.getTitle()
        );
    }

    public List<TreatmentActionResponseDTO> getActions(String planId) {
        getOrThrow(planId);
        return actionRepository.findByPlan_IdAndDeletedFalseOrderByCreatedAtAsc(planId)
                .stream()
                .map(TreatmentPlanMapper::toActionDTO)
                .toList();
    }

    @Transactional
    public void delete(String id) {
        TreatmentPlan plan = getOrThrow(id);
        if (plan.getStatus() == TreatmentPlanStatus.COMPLETED) {
            throw new AppException(ErrorCode.TREATMENT_PLAN_NOT_EDITABLE);
        }
        plan.setDeleted(true);
        planRepository.save(plan);
        saveHistory(plan, "Treatment plan deleted");
        auditService.log(
                AuditAction.TREATMENT_PLAN_DELETED,
                AuditEntityType.TREATMENT_PLAN,
                id,
                "Treatment plan deleted: " + plan.getTitle()
        );
    }

    public List<TreatmentPlanHistory> getHistory(String id) {
        getOrThrow(id);
        return historyRepository.findByPlan_IdOrderByCreatedAtDesc(id);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private void recalcProgress(TreatmentPlan plan) {
        // uses the exact method name from TreatmentActionRepository
        List<TreatmentAction> actions = actionRepository.findByPlan_IdAndDeletedFalseOrderByCreatedAtAsc(plan.getId());
        List<TreatmentAction> countable = actions.stream()
                .filter(a -> a.getStatus() != ActionStatus.CANCELLED)
                .toList();
        if (countable.isEmpty()) {
            plan.setProgressPct((short) 0);  // progressPct is Short, not int
        } else {
            long done = countable.stream().filter(a -> a.getStatus() == ActionStatus.DONE).count();
            plan.setProgressPct((short) ((done * 100) / countable.size()));
        }
        planRepository.save(plan);
    }

    private void computeTargetRisk(TreatmentPlan plan) {
        Short residual = plan.getScenario() != null ? plan.getScenario().getResidualRiskScore() : null;
        Short effectiveness = plan.getTreatmentEffectiveness();
        if (residual != null && effectiveness != null) {
            plan.setTargetRiskScore(TARGET_MATRIX[residual - 1][effectiveness - 1]);
        }
    }

    private TreatmentPlan getOrThrow(String id) {
        return planRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.TREATMENT_PLAN_NOT_FOUND));
    }

    private TreatmentAction getActionOrThrow(String id) {
        return actionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.TREATMENT_ACTION_NOT_FOUND));
    }

    private void saveHistory(TreatmentPlan plan, String summary) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id",                plan.getId());
        snapshot.put("title",             plan.getTitle());
        snapshot.put("description",       plan.getDescription());
        snapshot.put("scenarioId",        plan.getScenario() != null ? plan.getScenario().getId() : null);
        snapshot.put("assessmentId",      plan.getAssessment() != null ? plan.getAssessment().getId() : null);
        snapshot.put("treatmentStrategy", plan.getTreatmentStrategy() != null ? plan.getTreatmentStrategy().name() : null);
        snapshot.put("status",            plan.getStatus()      != null ? plan.getStatus().name()           : null);
        snapshot.put("createdById",        plan.getCreatedBy() != null ? plan.getCreatedBy().getId() : null);
        snapshot.put("dueDate",           plan.getDueDate()     != null ? plan.getDueDate().toString()      : null);
        snapshot.put("progressPct",            plan.getProgressPct());
        snapshot.put("treatmentEffectiveness", plan.getTreatmentEffectiveness());
        snapshot.put("targetRiskScore",        plan.getTargetRiskScore());
        snapshot.put("estimatedBudget",        plan.getEstimatedBudget());
        snapshot.put("actualCost",             plan.getActualCost());
        snapshot.put("createdAt",         plan.getCreatedAt()   != null ? plan.getCreatedAt().toString()    : null);
        snapshot.put("updatedAt",         plan.getUpdatedAt()   != null ? plan.getUpdatedAt().toString()    : null);

        TreatmentPlanHistory history = TreatmentPlanHistory.builder()
                .plan(plan)
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