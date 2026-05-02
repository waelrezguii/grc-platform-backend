package com.project.grcplatform.service;

import com.project.grcplatform.constant.*;
import com.project.grcplatform.dto.ArchiveRuleRequestDTO;
import com.project.grcplatform.dto.ArchiveRuleResponseDTO;
import com.project.grcplatform.dto.UnarchiveRequestDTO;
import com.project.grcplatform.exception.NotFoundException;
import com.project.grcplatform.model.*;
import com.project.grcplatform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArchiveService {

    private final ArchiveRuleRepository          archiveRuleRepository;
    private final VulnerabilityRepository        vulnerabilityRepository;
    private final RiskScenarioRepository         scenarioRepository;
    private final TreatmentPlanRepository        treatmentPlanRepository;
    private final AuditCampaignRepository        auditCampaignRepository;
    private final ComplianceEvaluationRepository evaluationRepository;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public List<ArchiveRuleResponseDTO> findAll() {
        return archiveRuleRepository.findAll().stream().map(this::toDTO).toList();
    }

    public ArchiveRuleResponseDTO findById(String id) {
        return toDTO(get(id));
    }

    @Transactional
    public ArchiveRuleResponseDTO create(ArchiveRuleRequestDTO request) {
        validateTriggerStatus(request.getEntityType(), request.getTriggerStatus());
        ArchiveRule rule = ArchiveRule.builder()
                .entityType(request.getEntityType())
                .triggerStatus(request.getTriggerStatus().toUpperCase())
                .delayDays(request.getDelayDays())
                .description(request.getDescription())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .build();
        return toDTO(archiveRuleRepository.save(rule));
    }

    @Transactional
    public ArchiveRuleResponseDTO update(String id, ArchiveRuleRequestDTO request) {
        ArchiveRule rule = get(id);
        if (request.getEntityType()    != null) rule.setEntityType(request.getEntityType());
        if (request.getTriggerStatus() != null) {
            validateTriggerStatus(rule.getEntityType(), request.getTriggerStatus());
            rule.setTriggerStatus(request.getTriggerStatus().toUpperCase());
        }
        if (request.getDelayDays()     != null) rule.setDelayDays(request.getDelayDays());
        if (request.getDescription()   != null) rule.setDescription(request.getDescription());
        if (request.getEnabled()       != null) rule.setEnabled(request.getEnabled());
        return toDTO(archiveRuleRepository.save(rule));
    }

    @Transactional
    public void delete(String id) {
        archiveRuleRepository.delete(get(id));
    }

    // ── Unarchive ─────────────────────────────────────────────────────────────

    @Transactional
    public void unarchive(UnarchiveRequestDTO request) {
        String id = request.getEntityId();
        switch (request.getEntityType()) {
            case VULNERABILITY -> {
                Vulnerability v = vulnerabilityRepository.findById(id)
                        .orElseThrow(() -> new NotFoundException("Vulnerability not found: " + id));
                if (!Boolean.TRUE.equals(v.getArchived()))
                    throw new IllegalStateException("Vulnerability " + id + " is not archived");
                v.setArchived(false);
                vulnerabilityRepository.save(v);
            }
            case RISK_SCENARIO -> {
                RiskScenario sc = scenarioRepository.findById(id)
                        .orElseThrow(() -> new NotFoundException("Risk scenario not found: " + id));
                if (!Boolean.TRUE.equals(sc.getArchived()))
                    throw new IllegalStateException("Risk scenario " + id + " is not archived");
                sc.setArchived(false);
                scenarioRepository.save(sc);
            }
            case TREATMENT_PLAN -> {
                TreatmentPlan tp = treatmentPlanRepository.findById(id)
                        .orElseThrow(() -> new NotFoundException("Treatment plan not found: " + id));
                if (!Boolean.TRUE.equals(tp.getArchived()))
                    throw new IllegalStateException("Treatment plan " + id + " is not archived");
                tp.setArchived(false);
                treatmentPlanRepository.save(tp);
            }
            case AUDIT_CAMPAIGN -> {
                AuditCampaign ac = auditCampaignRepository.findById(id)
                        .orElseThrow(() -> new NotFoundException("Audit campaign not found: " + id));
                if (!Boolean.TRUE.equals(ac.getArchived()))
                    throw new IllegalStateException("Audit campaign " + id + " is not archived");
                ac.setArchived(false);
                auditCampaignRepository.save(ac);
            }
            case COMPLIANCE_EVALUATION -> {
                ComplianceEvaluation ce = evaluationRepository.findById(id)
                        .orElseThrow(() -> new NotFoundException("Compliance evaluation not found: " + id));
                if (!Boolean.TRUE.equals(ce.getArchived()))
                    throw new IllegalStateException("Compliance evaluation " + id + " is not archived");
                ce.setArchived(false);
                evaluationRepository.save(ce);
            }
        }
        log.info("Unarchived {} [{}]", request.getEntityType(), id);
    }

    // ── Run ───────────────────────────────────────────────────────────────────

    /** Runs all enabled rules — called by the scheduler and by manual trigger. */
    @Transactional
    public void runAll() {
        List<ArchiveRule> rules = archiveRuleRepository.findAllByEnabledTrue();
        log.info("ArchiveService: running {} enabled rule(s)", rules.size());
        for (ArchiveRule rule : rules) {
            runRule(rule);
        }
    }

    /** Runs a single rule by id — manual trigger endpoint. */
    @Transactional
    public ArchiveRuleResponseDTO runOne(String id) {
        ArchiveRule rule = get(id);
        int count = runRule(rule);
        log.info("Manual archive run for rule [{}]: {} record(s) archived", rule.getId(), count);
        return toDTO(archiveRuleRepository.save(rule));
    }

    // ── Core logic ────────────────────────────────────────────────────────────

    private int runRule(ArchiveRule rule) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(rule.getDelayDays());
        int count;
        try {
            count = switch (rule.getEntityType()) {
                case VULNERABILITY        -> archiveVulnerabilities(rule.getTriggerStatus(), threshold);
                case RISK_SCENARIO        -> archiveScenarios(rule.getTriggerStatus(), threshold);
                case TREATMENT_PLAN       -> archiveTreatmentPlans(rule.getTriggerStatus(), threshold);
                case AUDIT_CAMPAIGN       -> archiveAuditCampaigns(rule.getTriggerStatus(), threshold);
                case COMPLIANCE_EVALUATION-> archiveEvaluations(rule.getTriggerStatus(), threshold);
            };
        } catch (Exception e) {
            log.error("Archive rule [{}] failed: {}", rule.getId(), e.getMessage(), e);
            count = 0;
        }

        rule.setLastRunAt(LocalDateTime.now());
        rule.setLastRunArchived(count);
        archiveRuleRepository.save(rule);

        if (count > 0) {
            log.info("Rule [{}|{}|{}d]: archived {} record(s)",
                    rule.getEntityType(), rule.getTriggerStatus(), rule.getDelayDays(), count);
        }
        return count;
    }

    private int archiveVulnerabilities(String status, LocalDateTime threshold) {
        VulnStatus s = VulnStatus.valueOf(status);
        List<Vulnerability> candidates = vulnerabilityRepository.findArchiveCandidates(s, threshold);
        candidates.forEach(v -> v.setArchived(true));
        vulnerabilityRepository.saveAll(candidates);
        return candidates.size();
    }

    private int archiveScenarios(String status, LocalDateTime threshold) {
        ScenarioStatus s = ScenarioStatus.valueOf(status);
        List<RiskScenario> candidates = scenarioRepository.findArchiveCandidates(s, threshold);
        candidates.forEach(sc -> sc.setArchived(true));
        scenarioRepository.saveAll(candidates);
        return candidates.size();
    }

    private int archiveTreatmentPlans(String status, LocalDateTime threshold) {
        TreatmentPlanStatus s = TreatmentPlanStatus.valueOf(status);
        List<TreatmentPlan> candidates = treatmentPlanRepository.findArchiveCandidates(s, threshold);
        candidates.forEach(p -> p.setArchived(true));
        treatmentPlanRepository.saveAll(candidates);
        return candidates.size();
    }

    private int archiveAuditCampaigns(String status, LocalDateTime threshold) {
        AuditCampaignStatus s = AuditCampaignStatus.valueOf(status);
        List<AuditCampaign> candidates = auditCampaignRepository.findArchiveCandidates(s, threshold);
        candidates.forEach(c -> c.setArchived(true));
        auditCampaignRepository.saveAll(candidates);
        return candidates.size();
    }

    private int archiveEvaluations(String status, LocalDateTime threshold) {
        EvaluationStatus s = EvaluationStatus.valueOf(status);
        List<ComplianceEvaluation> candidates = evaluationRepository.findArchiveCandidates(s, threshold);
        candidates.forEach(e -> e.setArchived(true));
        evaluationRepository.saveAll(candidates);
        return candidates.size();
    }

    // ── Validation ────────────────────────────────────────────────────────────

    /**
     * Validates that the triggerStatus string is a valid value for the given entity type.
     * Throws IllegalArgumentException if not — this surfaces as a 400 Bad Request.
     */
    private void validateTriggerStatus(ArchivableEntityType type, String status) {
        try {
            switch (type) {
                case VULNERABILITY         -> VulnStatus.valueOf(status.toUpperCase());
                case RISK_SCENARIO         -> ScenarioStatus.valueOf(status.toUpperCase());
                case TREATMENT_PLAN        -> TreatmentPlanStatus.valueOf(status.toUpperCase());
                case AUDIT_CAMPAIGN        -> AuditCampaignStatus.valueOf(status.toUpperCase());
                case COMPLIANCE_EVALUATION -> EvaluationStatus.valueOf(status.toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid trigger status '" + status + "' for entity type " + type.name() +
                    ". Valid values: " + validStatusValues(type));
        }
    }

    private String validStatusValues(ArchivableEntityType type) {
        return switch (type) {
            case VULNERABILITY         -> java.util.Arrays.toString(VulnStatus.values());
            case RISK_SCENARIO         -> java.util.Arrays.toString(ScenarioStatus.values());
            case TREATMENT_PLAN        -> java.util.Arrays.toString(TreatmentPlanStatus.values());
            case AUDIT_CAMPAIGN        -> java.util.Arrays.toString(AuditCampaignStatus.values());
            case COMPLIANCE_EVALUATION -> java.util.Arrays.toString(EvaluationStatus.values());
        };
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private ArchiveRule get(String id) {
        return archiveRuleRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Archive rule not found: " + id));
    }

    private ArchiveRuleResponseDTO toDTO(ArchiveRule r) {
        ArchiveRuleResponseDTO dto = new ArchiveRuleResponseDTO();
        dto.setId(r.getId());
        dto.setEntityType(r.getEntityType());
        dto.setTriggerStatus(r.getTriggerStatus());
        dto.setDelayDays(r.getDelayDays());
        dto.setDescription(r.getDescription());
        dto.setEnabled(Boolean.TRUE.equals(r.getEnabled()));
        dto.setLastRunAt(r.getLastRunAt());
        dto.setLastRunArchived(r.getLastRunArchived());
        dto.setCreatedAt(r.getCreatedAt());
        dto.setUpdatedAt(r.getUpdatedAt());
        return dto;
    }
}
