package com.project.grcplatform.service;

import com.project.grcplatform.constant.*;
import com.project.grcplatform.dto.*;
import com.project.grcplatform.exception.AppException;
import com.project.grcplatform.mapper.ComplianceEvaluationItemMapper;
import com.project.grcplatform.mapper.ComplianceEvaluationMapper;
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
public class ComplianceEvaluationService {

    private final ComplianceEvaluationRepository evaluationRepository;
    private final ComplianceEvaluationHistoryRepository historyRepository;
    private final ComplianceEvaluationItemRepository itemRepository;
    private final UserRepository userRepository;
    private final ComplianceFrameworkRepository frameworkRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    // ─── Evaluations ─────────────────────────────────────────────────────────────

    public Page<ComplianceEvaluationResponseDTO> findAll(String title, String frameworkId,
                                                         EvaluationStatus status, String evaluatorId,
                                                         boolean archived,
                                                         Pageable pageable) {
        return evaluationRepository.findAllWithFilters(title, frameworkId, status, evaluatorId, archived, pageable)
                .map(ComplianceEvaluationMapper::toDTO);
    }

    public ComplianceEvaluationResponseDTO findById(String id) {
        return ComplianceEvaluationMapper.toDTO(getOrThrow(id));
    }

    public List<ComplianceEvaluationHistory> getHistory(String id) {
        getOrThrow(id);
        return historyRepository.findByEvaluation_IdOrderByCreatedAtDesc(id);
    }

    public List<ComplianceEvaluationItemResponseDTO> getItems(String evaluationId) {
        getOrThrow(evaluationId);
        return itemRepository.findByEvaluation_IdAndDeletedFalse(evaluationId)
                .stream().map(ComplianceEvaluationItemMapper::toDTO).toList();
    }

    @Transactional
    public ComplianceEvaluationResponseDTO create(ComplianceEvaluationRequestDTO request) {
        ComplianceEvaluation e = ComplianceEvaluationMapper.toEntity(request);
        if (request.getFrameworkId() != null) frameworkRepository.findById(request.getFrameworkId()).ifPresent(e::setFramework);
        if (request.getEvaluatorId() != null) userRepository.findById(request.getEvaluatorId()).ifPresent(e::setEvaluator);
        String userId = getCurrentUserId();
        userRepository.findById(userId).ifPresent(e::setOwner);
        ComplianceEvaluation saved = evaluationRepository.saveAndFlush(e);
        ComplianceEvaluation fresh = evaluationRepository.findByIdAndDeletedFalse(saved.getId()).orElse(saved);
        saveHistory(fresh, "Evaluation created");
        auditService.log(AuditAction.COMPLIANCE_EVALUATION_CREATED, AuditEntityType.COMPLIANCE_EVALUATION,
                fresh.getId(), "Evaluation created: " + fresh.getTitle());
        return ComplianceEvaluationMapper.toDTO(fresh);
    }

    @Transactional
    public ComplianceEvaluationResponseDTO update(String id, ComplianceEvaluationRequestDTO request) {
        ComplianceEvaluation e = getOrThrow(id);
        if (e.getStatus() == EvaluationStatus.COMPLETED || e.getStatus() == EvaluationStatus.CANCELLED)
            throw new AppException(ErrorCode.COMPLIANCE_EVALUATION_NOT_EDITABLE);
        Map<String, Object> old = snapshot(e);
        ComplianceEvaluationMapper.updateEntity(e, request);
        ComplianceEvaluation saved = evaluationRepository.save(e);
        saveHistory(saved, "Evaluation updated");
        auditService.log(AuditAction.COMPLIANCE_EVALUATION_UPDATED, AuditEntityType.COMPLIANCE_EVALUATION,
                saved.getId(), "Evaluation updated: " + saved.getTitle(), old, snapshot(saved));
        return ComplianceEvaluationMapper.toDTO(saved);
    }

    @Transactional
    public ComplianceEvaluationResponseDTO start(String id) {
        ComplianceEvaluation e = getOrThrow(id);
        if (e.getStatus() != EvaluationStatus.PLANNED)
            throw new AppException(ErrorCode.COMPLIANCE_EVALUATION_INVALID_TRANSITION);
        e.setStatus(EvaluationStatus.IN_PROGRESS);
        ComplianceEvaluation saved = evaluationRepository.save(e);
        saveHistory(saved, "Evaluation started");
        auditService.log(AuditAction.COMPLIANCE_EVALUATION_STARTED, AuditEntityType.COMPLIANCE_EVALUATION,
                saved.getId(), "Evaluation started: " + saved.getTitle());
        return ComplianceEvaluationMapper.toDTO(saved);
    }

    @Transactional
    public ComplianceEvaluationResponseDTO complete(String id) {
        ComplianceEvaluation e = getOrThrow(id);
        if (e.getStatus() != EvaluationStatus.IN_PROGRESS)
            throw new AppException(ErrorCode.COMPLIANCE_EVALUATION_INVALID_TRANSITION);

        // Recalculate final score before completing
        recalculateScore(e);
        e.setStatus(EvaluationStatus.COMPLETED);
        ComplianceEvaluation saved = evaluationRepository.save(e);
        saveHistory(saved, "Evaluation completed — score: " + saved.getOverallScore());

        auditService.log(AuditAction.COMPLIANCE_EVALUATION_COMPLETED, AuditEntityType.COMPLIANCE_EVALUATION,
                saved.getId(), "Evaluation completed: " + saved.getTitle() + " — score: " + saved.getOverallScore());

        // Notify owner
        notificationService.notify(
                saved.getOwner() != null ? saved.getOwner().getId() : null,
                NotificationType.COMPLIANCE_EVALUATION_COMPLETED,
                NotificationEntityType.COMPLIANCE_EVALUATION,
                saved.getId(),
                "Évaluation de conformité terminée",
                "L'évaluation \"" + saved.getTitle() + "\" est terminée. Score global : "
                        + (saved.getOverallScore() != null ? String.format("%.1f", saved.getOverallScore()) : "N/A") + "%."
        );

        // Notify if score is critically low
        if (saved.getOverallScore() != null && saved.getOverallScore() < 50.0) {
            notificationService.notify(
                    saved.getOwner() != null ? saved.getOwner().getId() : null,
                    NotificationType.COMPLIANCE_SCORE_LOW,
                    NotificationEntityType.COMPLIANCE_EVALUATION,
                    saved.getId(),
                    "🔴 Score de conformité critique",
                    "L'évaluation \"" + saved.getTitle() + "\" a un score de conformité de "
                            + String.format("%.1f", saved.getOverallScore()) + "% — en dessous du seuil critique de 50%."
            );
        }

        return ComplianceEvaluationMapper.toDTO(saved);
    }

    @Transactional
    public ComplianceEvaluationResponseDTO cancel(String id) {
        ComplianceEvaluation e = getOrThrow(id);
        if (e.getStatus() == EvaluationStatus.COMPLETED || e.getStatus() == EvaluationStatus.CANCELLED)
            throw new AppException(ErrorCode.COMPLIANCE_EVALUATION_INVALID_TRANSITION);
        e.setStatus(EvaluationStatus.CANCELLED);
        ComplianceEvaluation saved = evaluationRepository.save(e);
        saveHistory(saved, "Evaluation cancelled");
        auditService.log(AuditAction.COMPLIANCE_EVALUATION_CANCELLED, AuditEntityType.COMPLIANCE_EVALUATION,
                saved.getId(), "Evaluation cancelled: " + saved.getTitle());
        return ComplianceEvaluationMapper.toDTO(saved);
    }

    @Transactional
    public void delete(String id) {
        ComplianceEvaluation e = getOrThrow(id);
        e.setDeleted(true);
        evaluationRepository.save(e);
        auditService.log(AuditAction.COMPLIANCE_EVALUATION_DELETED, AuditEntityType.COMPLIANCE_EVALUATION,
                id, "Evaluation deleted: " + e.getTitle());
    }

    // ─── Evaluation Items ─────────────────────────────────────────────────────────

    @Transactional
    public ComplianceEvaluationItemResponseDTO addItem(String evaluationId,
                                                       ComplianceEvaluationItemRequestDTO request) {
        ComplianceEvaluation e = getOrThrow(evaluationId);
        if (e.getStatus() == EvaluationStatus.COMPLETED || e.getStatus() == EvaluationStatus.CANCELLED)
            throw new AppException(ErrorCode.COMPLIANCE_EVALUATION_NOT_EDITABLE);

        ComplianceEvaluationItem item = ComplianceEvaluationItemMapper.toEntity(request, evaluationId);
        ComplianceEvaluationItem saved = itemRepository.save(item);

        // Recalculate overall score on the evaluation
        recalculateScore(e);
        evaluationRepository.save(e);

        auditService.log(AuditAction.COMPLIANCE_ITEM_ADDED, AuditEntityType.COMPLIANCE_EVALUATION,
                evaluationId, "Item added for requirement: " + request.getRequirementId());

        return ComplianceEvaluationItemMapper.toDTO(saved);
    }

    @Transactional
    public ComplianceEvaluationItemResponseDTO updateItem(String evaluationId, String itemId,
                                                          ComplianceEvaluationItemRequestDTO request) {
        ComplianceEvaluation e = getOrThrow(evaluationId);
        if (e.getStatus() == EvaluationStatus.COMPLETED || e.getStatus() == EvaluationStatus.CANCELLED)
            throw new AppException(ErrorCode.COMPLIANCE_EVALUATION_NOT_EDITABLE);

        ComplianceEvaluationItem item = itemRepository.findByIdAndDeletedFalse(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.COMPLIANCE_EVALUATION_ITEM_NOT_FOUND));

        ComplianceEvaluationItemMapper.updateEntity(item, request);
        ComplianceEvaluationItem saved = itemRepository.save(item);

        recalculateScore(e);
        evaluationRepository.save(e);

        return ComplianceEvaluationItemMapper.toDTO(saved);
    }

    @Transactional
    public void deleteItem(String evaluationId, String itemId) {
        ComplianceEvaluation e = getOrThrow(evaluationId);
        if (e.getStatus() == EvaluationStatus.COMPLETED || e.getStatus() == EvaluationStatus.CANCELLED)
            throw new AppException(ErrorCode.COMPLIANCE_EVALUATION_NOT_EDITABLE);

        ComplianceEvaluationItem item = itemRepository.findByIdAndDeletedFalse(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.COMPLIANCE_EVALUATION_ITEM_NOT_FOUND));
        item.setDeleted(true);
        itemRepository.save(item);

        recalculateScore(e);
        evaluationRepository.save(e);
    }

    // ─── Score Calculation ───────────────────────────────────────────────────────

    /**
     * Calculates the overall compliance score as the average of all item scores
     * excluding NOT_APPLICABLE items (score = null).
     */
    private void recalculateScore(ComplianceEvaluation evaluation) {
        List<Double> scores = itemRepository.findScoresByEvaluationId(evaluation.getId());
        if (scores.isEmpty()) {
            evaluation.setOverallScore(null);
        } else {
            double avg = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            evaluation.setOverallScore(Math.round(avg * 10.0) / 10.0);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private ComplianceEvaluation getOrThrow(String id) {
        return evaluationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.COMPLIANCE_EVALUATION_NOT_FOUND));
    }

    private void saveHistory(ComplianceEvaluation e, String summary) {
        historyRepository.save(ComplianceEvaluationHistory.builder()
                .evaluation(e)
                .changeSummary(summary).snapshot(snapshot(e)).build());
    }

    private Map<String, Object> snapshot(ComplianceEvaluation e) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", e.getId()); m.put("frameworkId", e.getFramework() != null ? e.getFramework().getId() : null); m.put("title", e.getTitle());
        m.put("description", e.getDescription()); m.put("scope", e.getScope());
        m.put("status", e.getStatus() != null ? e.getStatus().name() : null);
        m.put("evaluatorId", e.getEvaluator() != null ? e.getEvaluator().getId() : null); m.put("overallScore", e.getOverallScore());
        m.put("startDate", e.getStartDate() != null ? e.getStartDate().toString() : null);
        m.put("endDate", e.getEndDate() != null ? e.getEndDate().toString() : null);
        m.put("ownerId", e.getOwner() != null ? e.getOwner().getId() : null);
        return m;
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthToken j) return j.getUserId();
        return "system";
    }
}