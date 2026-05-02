package com.project.grcplatform.service;

import com.project.grcplatform.constant.*;
import com.project.grcplatform.dto.GovernancePolicyRequestDTO;
import com.project.grcplatform.dto.GovernancePolicyResponseDTO;
import com.project.grcplatform.exception.AppException;
import com.project.grcplatform.exception.NotFoundException;
import com.project.grcplatform.mapper.GovernancePolicyMapper;
import com.project.grcplatform.model.GovernancePolicy;
import com.project.grcplatform.model.GovernancePolicyHistory;
import com.project.grcplatform.model.PolicyType;
import com.project.grcplatform.repository.GovernancePolicyHistoryRepository;
import com.project.grcplatform.repository.GovernancePolicyRepository;
import com.project.grcplatform.repository.PolicyTypeRepository;
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
public class GovernancePolicyService {

    private final GovernancePolicyRepository policyRepository;
    private final GovernancePolicyHistoryRepository historyRepository;
    private final PolicyTypeRepository policyTypeRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    // ─── Read ────────────────────────────────────────────────────────────────────

    public Page<GovernancePolicyResponseDTO> findAll(String title, String policyTypeId,
                                                     PolicyStatus status, String ownerId,
                                                     Pageable pageable) {
        return policyRepository.findAllWithFilters(title, policyTypeId, status, ownerId, pageable)
                .map(GovernancePolicyMapper::toDTO);
    }

    public GovernancePolicyResponseDTO findById(String id) {
        return GovernancePolicyMapper.toDTO(getOrThrow(id));
    }

    public List<GovernancePolicyHistory> getHistory(String id) {
        getOrThrow(id);
        return historyRepository.findByPolicy_IdOrderByCreatedAtDesc(id);
    }

    // ─── Write ───────────────────────────────────────────────────────────────────

    @Transactional
    public GovernancePolicyResponseDTO create(GovernancePolicyRequestDTO request) {
        GovernancePolicy policy = GovernancePolicyMapper.toEntity(request);

        if (request.getPolicyTypeId() != null) {
            PolicyType policyType = policyTypeRepository.findById(request.getPolicyTypeId())
                    .orElseThrow(() -> new NotFoundException("Policy type not found: " + request.getPolicyTypeId()));
            policy.setPolicyType(policyType);
        }

        GovernancePolicy saved = policyRepository.saveAndFlush(policy);
        GovernancePolicy fresh = policyRepository.findByIdAndDeletedFalse(saved.getId()).orElse(saved);
        saveHistory(fresh, "Policy created");

        auditService.log(AuditAction.GOVERNANCE_POLICY_CREATED, AuditEntityType.GOVERNANCE_POLICY,
                fresh.getId(), "Governance policy created: " + fresh.getTitle());

        return GovernancePolicyMapper.toDTO(fresh);
    }

    @Transactional
    public GovernancePolicyResponseDTO update(String id, GovernancePolicyRequestDTO request) {
        GovernancePolicy policy = getOrThrow(id);
        if (policy.getStatus() == PolicyStatus.DEPRECATED) {
            throw new AppException(ErrorCode.GOVERNANCE_POLICY_INVALID_TRANSITION);
        }
        Map<String, Object> old = snapshot(policy);
        GovernancePolicyMapper.updateEntity(policy, request);

        if (request.getPolicyTypeId() != null) {
            PolicyType policyType = policyTypeRepository.findById(request.getPolicyTypeId())
                    .orElseThrow(() -> new NotFoundException("Policy type not found: " + request.getPolicyTypeId()));
            policy.setPolicyType(policyType);
        }

        GovernancePolicy saved = policyRepository.save(policy);
        saveHistory(saved, "Policy updated");

        auditService.log(AuditAction.GOVERNANCE_POLICY_UPDATED, AuditEntityType.GOVERNANCE_POLICY,
                saved.getId(), "Governance policy updated: " + saved.getTitle(), old, snapshot(saved));

        return GovernancePolicyMapper.toDTO(saved);
    }

    @Transactional
    public GovernancePolicyResponseDTO submit(String id) {
        GovernancePolicy policy = getOrThrow(id);
        if (policy.getStatus() != PolicyStatus.DRAFT) {
            throw new AppException(ErrorCode.GOVERNANCE_POLICY_INVALID_TRANSITION);
        }
        policy.setStatus(PolicyStatus.UNDER_REVIEW);
        GovernancePolicy saved = policyRepository.save(policy);
        saveHistory(saved, "Policy submitted for review");

        auditService.log(AuditAction.GOVERNANCE_POLICY_SUBMITTED, AuditEntityType.GOVERNANCE_POLICY,
                saved.getId(), "Policy submitted: " + saved.getTitle());

        return GovernancePolicyMapper.toDTO(saved);
    }

    @Transactional
    public GovernancePolicyResponseDTO approve(String id) {
        GovernancePolicy policy = getOrThrow(id);
        if (policy.getStatus() != PolicyStatus.UNDER_REVIEW) {
            throw new AppException(ErrorCode.GOVERNANCE_POLICY_INVALID_TRANSITION);
        }
        policy.setStatus(PolicyStatus.APPROVED);
        GovernancePolicy saved = policyRepository.save(policy);
        saveHistory(saved, "Policy approved");

        auditService.log(AuditAction.GOVERNANCE_POLICY_APPROVED, AuditEntityType.GOVERNANCE_POLICY,
                saved.getId(), "Policy approved: " + saved.getTitle());

        // Notify policy owner
        notificationService.notify(
                saved.getCreatedBy() != null ? saved.getCreatedBy().getId() : null,
                NotificationType.GOVERNANCE_POLICY_APPROVED,
                NotificationEntityType.GOVERNANCE_POLICY,
                saved.getId(),
                "Politique approuvée",
                "La politique \"" + saved.getTitle() + "\" a été approuvée."
        );

        return GovernancePolicyMapper.toDTO(saved);
    }

    @Transactional
    public GovernancePolicyResponseDTO deprecate(String id) {
        GovernancePolicy policy = getOrThrow(id);
        if (policy.getStatus() != PolicyStatus.APPROVED) {
            throw new AppException(ErrorCode.GOVERNANCE_POLICY_INVALID_TRANSITION);
        }
        policy.setStatus(PolicyStatus.DEPRECATED);
        GovernancePolicy saved = policyRepository.save(policy);
        saveHistory(saved, "Policy deprecated");

        auditService.log(AuditAction.GOVERNANCE_POLICY_DEPRECATED, AuditEntityType.GOVERNANCE_POLICY,
                saved.getId(), "Policy deprecated: " + saved.getTitle());

        return GovernancePolicyMapper.toDTO(saved);
    }

    @Transactional
    public void delete(String id) {
        GovernancePolicy policy = getOrThrow(id);
        policy.setDeleted(true);
        policyRepository.save(policy);
        saveHistory(policy, "Policy deleted");

        auditService.log(AuditAction.GOVERNANCE_POLICY_DELETED, AuditEntityType.GOVERNANCE_POLICY,
                id, "Policy deleted: " + policy.getTitle());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private GovernancePolicy getOrThrow(String id) {
        return policyRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.GOVERNANCE_POLICY_NOT_FOUND));
    }

    private void saveHistory(GovernancePolicy policy, String summary) {
        GovernancePolicyHistory h = GovernancePolicyHistory.builder()
                .policy(policy)
                .changeSummary(summary)
                .snapshot(snapshot(policy))
                .build();
        historyRepository.save(h);
    }

    private Map<String, Object> snapshot(GovernancePolicy p) {
        Map<String, Object> map = new HashMap<>();
        map.put("id",            p.getId());
        map.put("title",         p.getTitle());
        map.put("description",   p.getDescription());
        map.put("policyType",    p.getPolicyType()    != null ? p.getPolicyType().getName()    : null);
        map.put("status",        p.getStatus()        != null ? p.getStatus().name()        : null);
        map.put("version",       p.getVersion());
        map.put("effectiveDate", p.getEffectiveDate() != null ? p.getEffectiveDate().toString() : null);
        map.put("expiryDate",    p.getExpiryDate()    != null ? p.getExpiryDate().toString()    : null);
        map.put("createdById",   p.getCreatedBy() != null ? p.getCreatedBy().getId() : null);
        return map;
    }

}
