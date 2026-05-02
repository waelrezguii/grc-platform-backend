package com.project.grcplatform.service;

import com.project.grcplatform.constant.*;
import com.project.grcplatform.dto.AddScenarioRequestDTO;
import com.project.grcplatform.dto.RiskAssessmentRequestDTO;
import com.project.grcplatform.dto.RiskAssessmentResponseDTO;
import com.project.grcplatform.exception.AppException;
import com.project.grcplatform.mapper.RiskAssessmentMapper;
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
public class RiskAssessmentService {

    private final RiskAssessmentRepository assessmentRepository;
    private final RiskAssessmentScenarioRepository assessmentScenarioRepository;
    private final RiskAssessmentHistoryRepository historyRepository;
    private final RiskScenarioRepository scenarioRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final OrganisationRepository organisationRepository;
    private final UserRepository userRepository;

    public Page<RiskAssessmentResponseDTO> findAll(String title, String organisationId,
                                                   AssessmentStatus status, Pageable pageable) {
        return assessmentRepository
                .findAllWithFilters(title, organisationId, status, pageable)
                .map(RiskAssessmentMapper::toDTO);
    }

    public RiskAssessmentResponseDTO findById(String id) {
        return RiskAssessmentMapper.toDTO(getOrThrow(id));
    }

    @Transactional
    public RiskAssessmentResponseDTO create(RiskAssessmentRequestDTO request) {
        RiskAssessment assessment = RiskAssessmentMapper.toEntity(request);
        Organisation organisation = organisationRepository.findById(request.getOrganisationId()).orElseThrow(() -> new RuntimeException("Organisation not found"));
        assessment.setOrganisation(organisation);
        if (request.getRiskOwnerId() != null) {
            userRepository.findById(request.getRiskOwnerId()).ifPresent(assessment::setRiskOwner);
        }
        RiskAssessment saved = assessmentRepository.saveAndFlush(assessment);
        RiskAssessment fresh = assessmentRepository.findByIdAndDeletedFalse(saved.getId()).orElse(saved);
        saveHistory(fresh, "Assessment created");

        auditService.log(AuditAction.ASSESSMENT_CREATED, AuditEntityType.ASSESSMENT,
                fresh.getId(), "Assessment created: " + fresh.getTitle());

        // Notify risk owner that an assessment has been assigned to them
        if (fresh.getRiskOwner() != null) {
            notificationService.notify(
                    fresh.getRiskOwner().getId(),
                    NotificationType.ASSESSMENT_CREATED,
                    NotificationEntityType.ASSESSMENT,
                    fresh.getId(),
                    "New Risk Assessment Created",
                    "You have been assigned as risk owner for assessment \"" + fresh.getTitle() + "\"."
            );
        }

        return RiskAssessmentMapper.toDTO(fresh);
    }

    @Transactional
    public RiskAssessmentResponseDTO update(String id, RiskAssessmentRequestDTO request) {
        RiskAssessment assessment = getOrThrow(id);
        if (assessment.getStatus() == AssessmentStatus.COMPLETED ||
                assessment.getStatus() == AssessmentStatus.ARCHIVED) {
            throw new AppException(ErrorCode.ASSESSMENT_NOT_EDITABLE);
        }
        Map<String, Object> oldValues = Map.of(
                "title",  assessment.getTitle() != null ? assessment.getTitle() : "",
                "status", assessment.getStatus() != null ? assessment.getStatus().name() : ""
        );
        RiskAssessmentMapper.updateEntity(assessment, request);
        if (request.getOrganisationId() != null) {
            organisationRepository.findById(request.getOrganisationId()).ifPresent(assessment::setOrganisation);
        }
        if (request.getRiskOwnerId() != null) {
            userRepository.findById(request.getRiskOwnerId()).ifPresent(assessment::setRiskOwner);
        }
        RiskAssessment saved = assessmentRepository.save(assessment);
        saveHistory(saved, "Assessment updated");

        auditService.log(AuditAction.ASSESSMENT_UPDATED, AuditEntityType.ASSESSMENT,
                saved.getId(), "Assessment updated: " + saved.getTitle(),
                oldValues,
                Map.of("title", saved.getTitle() != null ? saved.getTitle() : "",
                        "status", saved.getStatus() != null ? saved.getStatus().name() : ""));
        return RiskAssessmentMapper.toDTO(saved);
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────────

    @Transactional
    public RiskAssessmentResponseDTO start(String id) {
        RiskAssessment assessment = getOrThrow(id);
        if (assessment.getStatus() != AssessmentStatus.PLANNED) {
            throw new AppException(ErrorCode.ASSESSMENT_INVALID_TRANSITION);
        }
        assessment.setStatus(AssessmentStatus.IN_PROGRESS);
        RiskAssessment saved = assessmentRepository.save(assessment);
        saveHistory(saved, "Assessment started");
        auditService.log(AuditAction.ASSESSMENT_STARTED, AuditEntityType.ASSESSMENT,
                saved.getId(), "Assessment started: " + saved.getTitle());
        notifyStatusChange(saved);
        return RiskAssessmentMapper.toDTO(saved);
    }

    @Transactional
    public RiskAssessmentResponseDTO submitForReview(String id) {
        RiskAssessment assessment = getOrThrow(id);
        if (assessment.getStatus() != AssessmentStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.ASSESSMENT_INVALID_TRANSITION);
        }
        assessment.setStatus(AssessmentStatus.UNDER_REVIEW);
        RiskAssessment saved = assessmentRepository.save(assessment);
        saveHistory(saved, "Assessment submitted for review");
        auditService.log(AuditAction.ASSESSMENT_SUBMITTED, AuditEntityType.ASSESSMENT,
                saved.getId(), "Assessment submitted for review: " + saved.getTitle());
        notifyStatusChange(saved);
        return RiskAssessmentMapper.toDTO(saved);
    }

    @Transactional
    public RiskAssessmentResponseDTO complete(String id) {
        RiskAssessment assessment = getOrThrow(id);
        if (assessment.getStatus() != AssessmentStatus.UNDER_REVIEW) {
            throw new AppException(ErrorCode.ASSESSMENT_INVALID_TRANSITION);
        }
        String currentUser = getCurrentUserId();
        assessment.setStatus(AssessmentStatus.COMPLETED);
        assessment.setReviewedBy(currentUser);
        assessment.setReviewedAt(LocalDateTime.now());
        assessment.setApprovedBy(currentUser);
        assessment.setApprovedAt(LocalDateTime.now());
        recomputeScores(assessment);
        RiskAssessment saved = assessmentRepository.save(assessment);
        saveHistory(saved, "Assessment completed and approved");
        auditService.log(AuditAction.ASSESSMENT_COMPLETED, AuditEntityType.ASSESSMENT,
                saved.getId(), "Assessment completed: " + saved.getTitle());
        notifyStatusChange(saved);
        return RiskAssessmentMapper.toDTO(saved);
    }

    @Transactional
    public RiskAssessmentResponseDTO archive(String id) {
        RiskAssessment assessment = getOrThrow(id);
        if (assessment.getStatus() != AssessmentStatus.COMPLETED) {
            throw new AppException(ErrorCode.ASSESSMENT_INVALID_TRANSITION);
        }
        assessment.setStatus(AssessmentStatus.ARCHIVED);
        RiskAssessment saved = assessmentRepository.save(assessment);
        saveHistory(saved, "Assessment archived");
        auditService.log(AuditAction.ASSESSMENT_ARCHIVED, AuditEntityType.ASSESSMENT,
                saved.getId(), "Assessment archived: " + saved.getTitle());
        notifyStatusChange(saved);
        return RiskAssessmentMapper.toDTO(saved);
    }

    // ─── Scenario management ─────────────────────────────────────────────────────

    @Transactional
    public RiskAssessmentResponseDTO addScenario(String assessmentId, AddScenarioRequestDTO request) {
        RiskAssessment assessment = getOrThrow(assessmentId);
        if (assessment.getStatus() == AssessmentStatus.COMPLETED ||
                assessment.getStatus() == AssessmentStatus.ARCHIVED) {
            throw new AppException(ErrorCode.ASSESSMENT_NOT_EDITABLE);
        }
        if (assessmentScenarioRepository.existsByAssessment_IdAndScenario_Id(assessmentId, request.getScenarioId())) {
            throw new AppException(ErrorCode.SCENARIO_ALREADY_IN_ASSESSMENT);
        }
        RiskScenario scenarioEntity = scenarioRepository.findByIdAndDeletedFalse(request.getScenarioId())
                .orElseThrow(() -> new AppException(ErrorCode.SCENARIO_NOT_FOUND));

        RiskAssessmentScenario link = RiskAssessmentScenario.builder()
                .assessment(assessment)
                .scenario(scenarioEntity)
                .treatmentStrategy(request.getTreatmentStrategy())
                .treatmentNotes(request.getTreatmentNotes())
                .addedBy(getCurrentUserId())
                .build();
        assessmentScenarioRepository.save(link);
        recomputeScores(assessment);
        RiskAssessment saved = assessmentRepository.save(assessment);
        saveHistory(saved, "Scenario " + request.getScenarioId() + " added");
        auditService.log(AuditAction.ASSESSMENT_SCENARIO_ADDED, AuditEntityType.ASSESSMENT,
                assessmentId, "Scenario " + request.getScenarioId() + " added to assessment");
        return RiskAssessmentMapper.toDTO(saved);
    }

    @Transactional
    public RiskAssessmentResponseDTO removeScenario(String assessmentId, String scenarioId) {
        RiskAssessment assessment = getOrThrow(assessmentId);
        if (assessment.getStatus() == AssessmentStatus.COMPLETED ||
                assessment.getStatus() == AssessmentStatus.ARCHIVED) {
            throw new AppException(ErrorCode.ASSESSMENT_NOT_EDITABLE);
        }
        assessmentScenarioRepository.deleteByAssessment_IdAndScenario_Id(assessmentId, scenarioId);
        recomputeScores(assessment);
        RiskAssessment saved = assessmentRepository.save(assessment);
        saveHistory(saved, "Scenario " + scenarioId + " removed");
        auditService.log(AuditAction.ASSESSMENT_SCENARIO_REMOVED, AuditEntityType.ASSESSMENT,
                assessmentId, "Scenario " + scenarioId + " removed from assessment");
        return RiskAssessmentMapper.toDTO(saved);
    }

    public List<RiskAssessmentScenario> getScenarios(String assessmentId) {
        getOrThrow(assessmentId);
        return assessmentScenarioRepository.findByAssessment_Id(assessmentId);
    }

    @Transactional
    public void delete(String id) {
        RiskAssessment assessment = getOrThrow(id);
        if (assessment.getStatus() == AssessmentStatus.COMPLETED ||
                assessment.getStatus() == AssessmentStatus.ARCHIVED) {
            throw new AppException(ErrorCode.ASSESSMENT_NOT_EDITABLE);
        }
        assessment.setDeleted(true);
        assessmentRepository.save(assessment);
        saveHistory(assessment, "Assessment deleted");
        auditService.log(AuditAction.ASSESSMENT_DELETED, AuditEntityType.ASSESSMENT,
                id, "Assessment deleted: " + assessment.getTitle());
    }

    public List<RiskAssessmentHistory> getHistory(String id) {
        getOrThrow(id);
        return historyRepository.findByAssessment_IdOrderByCreatedAtDesc(id);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private void notifyStatusChange(RiskAssessment assessment) {
        if (assessment.getRiskOwner() == null) return;
        notificationService.notify(
                assessment.getRiskOwner().getId(),
                NotificationType.ASSESSMENT_STATUS_CHANGED,
                NotificationEntityType.ASSESSMENT,
                assessment.getId(),
                "Assessment Status Updated",
                "Risk Assessment \"" + assessment.getTitle() + "\" is now " + assessment.getStatus().name() + "."
        );
    }

    private void recomputeScores(RiskAssessment assessment) {
        List<RiskAssessmentScenario> links = assessmentScenarioRepository.findByAssessment_Id(assessment.getId());
        if (links.isEmpty()) {
            assessment.setOverallRiskScore(null);
            assessment.setOverallResidualScore(null);
            return;
        }
        double avgRaw = links.stream()
                .map(link -> scenarioRepository.findByIdAndDeletedFalse(link.getScenario().getId()))
                .filter(java.util.Optional::isPresent)
                .mapToInt(opt -> {
                    RiskScenario s = opt.get();
                    return s.getRawRiskScore() != null ? s.getRawRiskScore() : 0;
                })
                .average().orElse(0);

        double avgResidual = links.stream()
                .map(link -> scenarioRepository.findByIdAndDeletedFalse(link.getScenario().getId()))
                .filter(java.util.Optional::isPresent)
                .mapToInt(opt -> {
                    RiskScenario s = opt.get();
                    return s.getResidualRiskScore() != null ? s.getResidualRiskScore() : 0;
                })
                .average().orElse(0);

        assessment.setOverallRiskScore((short) Math.round(avgRaw));
        assessment.setOverallResidualScore((short) Math.round(avgResidual));
    }

    private RiskAssessment getOrThrow(String id) {
        return assessmentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));
    }

    private void saveHistory(RiskAssessment assessment, String summary) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", assessment.getId());
        snapshot.put("title", assessment.getTitle());
        snapshot.put("description", assessment.getDescription());
        snapshot.put("organisationId", assessment.getOrganisation() != null ? assessment.getOrganisation().getId() : null);
        snapshot.put("startDate", assessment.getStartDate() != null ? assessment.getStartDate().toString() : null);
        snapshot.put("endDate", assessment.getEndDate() != null ? assessment.getEndDate().toString() : null);
        snapshot.put("status", assessment.getStatus() != null ? assessment.getStatus().name() : null);
        snapshot.put("overallRiskScore", assessment.getOverallRiskScore());
        snapshot.put("overallResidualScore", assessment.getOverallResidualScore());
        snapshot.put("treatmentStrategy", assessment.getTreatmentStrategy() != null ? assessment.getTreatmentStrategy().name() : null);
        snapshot.put("treatmentNotes", assessment.getTreatmentNotes());
        snapshot.put("riskOwnerId", assessment.getRiskOwner() != null ? assessment.getRiskOwner().getId() : null);
        snapshot.put("reviewedBy", assessment.getReviewedBy());
        snapshot.put("reviewedAt", assessment.getReviewedAt() != null ? assessment.getReviewedAt().toString() : null);
        snapshot.put("approvedBy", assessment.getApprovedBy());
        snapshot.put("approvedAt", assessment.getApprovedAt() != null ? assessment.getApprovedAt().toString() : null);
        snapshot.put("createdBy", assessment.getCreatedBy() != null ? assessment.getCreatedBy().getId() : null);
        snapshot.put("createdAt", assessment.getCreatedAt() != null ? assessment.getCreatedAt().toString() : null);
        snapshot.put("updatedAt", assessment.getUpdatedAt() != null ? assessment.getUpdatedAt().toString() : null);

        RiskAssessmentHistory history = RiskAssessmentHistory.builder()
                .assessment(assessment)
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