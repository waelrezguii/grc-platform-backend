package com.project.grcplatform.service;

import com.project.grcplatform.constant.*;
import com.project.grcplatform.dto.*;
import com.project.grcplatform.exception.AppException;
import com.project.grcplatform.mapper.ComplianceFrameworkMapper;
import com.project.grcplatform.mapper.ComplianceRequirementMapper;
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
public class ComplianceFrameworkService {

    private final ComplianceFrameworkRepository frameworkRepository;
    private final ComplianceFrameworkHistoryRepository historyRepository;
    private final ComplianceRequirementRepository requirementRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    // ─── Frameworks ──────────────────────────────────────────────────────────────

    public Page<ComplianceFrameworkResponseDTO> findAll(String name, FrameworkType type,
                                                        FrameworkStatus status, Pageable pageable) {
        return frameworkRepository.findAllWithFilters(name, type, status, pageable)
                .map(ComplianceFrameworkMapper::toDTO);
    }

    public ComplianceFrameworkResponseDTO findById(String id) {
        return ComplianceFrameworkMapper.toDTO(getFrameworkOrThrow(id));
    }

    public List<ComplianceFrameworkHistory> getHistory(String id) {
        getFrameworkOrThrow(id);
        return historyRepository.findByFramework_IdOrderByCreatedAtDesc(id);
    }

    public List<ComplianceRequirementResponseDTO> getRequirements(String frameworkId) {
        getFrameworkOrThrow(frameworkId);
        return requirementRepository
                .findByFramework_IdAndDeletedFalseOrderByCodeAsc(frameworkId)
                .stream().map(ComplianceRequirementMapper::toDTO).toList();
    }

    @Transactional
    public ComplianceFrameworkResponseDTO create(ComplianceFrameworkRequestDTO request) {
        ComplianceFramework f = ComplianceFrameworkMapper.toEntity(request);
        String userId = getCurrentUserId();
        userRepository.findById(userId).ifPresent(f::setOwner);
        ComplianceFramework saved = frameworkRepository.saveAndFlush(f);
        ComplianceFramework fresh = frameworkRepository.findByIdAndDeletedFalse(saved.getId()).orElse(saved);
        saveHistory(fresh, "Framework created");
        auditService.log(AuditAction.COMPLIANCE_FRAMEWORK_CREATED, AuditEntityType.COMPLIANCE_FRAMEWORK,
                fresh.getId(), "Framework created: " + fresh.getName());
        return ComplianceFrameworkMapper.toDTO(fresh);
    }

    @Transactional
    public ComplianceFrameworkResponseDTO update(String id, ComplianceFrameworkRequestDTO request) {
        ComplianceFramework f = getFrameworkOrThrow(id);
        if (f.getStatus() == FrameworkStatus.DEPRECATED)
            throw new AppException(ErrorCode.COMPLIANCE_FRAMEWORK_INVALID_TRANSITION);
        Map<String, Object> old = snapshot(f);
        ComplianceFrameworkMapper.updateEntity(f, request);
        ComplianceFramework saved = frameworkRepository.save(f);
        saveHistory(saved, "Framework updated");
        auditService.log(AuditAction.COMPLIANCE_FRAMEWORK_UPDATED, AuditEntityType.COMPLIANCE_FRAMEWORK,
                saved.getId(), "Framework updated: " + saved.getName(), old, snapshot(saved));
        return ComplianceFrameworkMapper.toDTO(saved);
    }

    @Transactional
    public ComplianceFrameworkResponseDTO deprecate(String id) {
        ComplianceFramework f = getFrameworkOrThrow(id);
        if (f.getStatus() == FrameworkStatus.DEPRECATED)
            throw new AppException(ErrorCode.COMPLIANCE_FRAMEWORK_INVALID_TRANSITION);
        f.setStatus(FrameworkStatus.DEPRECATED);
        ComplianceFramework saved = frameworkRepository.save(f);
        saveHistory(saved, "Framework deprecated");
        auditService.log(AuditAction.COMPLIANCE_FRAMEWORK_DEPRECATED, AuditEntityType.COMPLIANCE_FRAMEWORK,
                saved.getId(), "Framework deprecated: " + saved.getName());
        return ComplianceFrameworkMapper.toDTO(saved);
    }

    @Transactional
    public void delete(String id) {
        ComplianceFramework f = getFrameworkOrThrow(id);
        f.setDeleted(true);
        frameworkRepository.save(f);
        auditService.log(AuditAction.COMPLIANCE_FRAMEWORK_DELETED, AuditEntityType.COMPLIANCE_FRAMEWORK,
                id, "Framework deleted: " + f.getName());
    }

    // ─── Requirements ─────────────────────────────────────────────────────────────

    @Transactional
    public ComplianceRequirementResponseDTO createRequirement(ComplianceRequirementRequestDTO request) {
        ComplianceFramework fw = getFrameworkOrThrow(request.getFrameworkId());
        ComplianceRequirement r = ComplianceRequirementMapper.toEntity(request);
        r.setFramework(fw);
        ComplianceRequirement saved = requirementRepository.save(r);
        auditService.log(AuditAction.COMPLIANCE_REQUIREMENT_CREATED, AuditEntityType.COMPLIANCE_REQUIREMENT,
                saved.getId(), "Requirement created: " + saved.getCode() + " - " + saved.getTitle());
        return ComplianceRequirementMapper.toDTO(saved);
    }

    @Transactional
    public ComplianceRequirementResponseDTO updateRequirement(String id, ComplianceRequirementRequestDTO request) {
        ComplianceRequirement r = requirementRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.COMPLIANCE_REQUIREMENT_NOT_FOUND));
        ComplianceRequirementMapper.updateEntity(r, request);
        ComplianceRequirement saved = requirementRepository.save(r);
        auditService.log(AuditAction.COMPLIANCE_REQUIREMENT_UPDATED, AuditEntityType.COMPLIANCE_REQUIREMENT,
                saved.getId(), "Requirement updated: " + saved.getCode());
        return ComplianceRequirementMapper.toDTO(saved);
    }

    @Transactional
    public void deleteRequirement(String id) {
        ComplianceRequirement r = requirementRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.COMPLIANCE_REQUIREMENT_NOT_FOUND));
        r.setDeleted(true);
        requirementRepository.save(r);
        auditService.log(AuditAction.COMPLIANCE_REQUIREMENT_DELETED, AuditEntityType.COMPLIANCE_REQUIREMENT,
                id, "Requirement deleted: " + r.getCode());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private ComplianceFramework getFrameworkOrThrow(String id) {
        return frameworkRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.COMPLIANCE_FRAMEWORK_NOT_FOUND));
    }

    private void saveHistory(ComplianceFramework f, String summary) {
        historyRepository.save(ComplianceFrameworkHistory.builder()
                .framework(f)
                .changeSummary(summary).snapshot(snapshot(f)).build());
    }

    private Map<String, Object> snapshot(ComplianceFramework f) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", f.getId()); m.put("name", f.getName()); m.put("description", f.getDescription());
        m.put("frameworkType", f.getFrameworkType() != null ? f.getFrameworkType().name() : null);
        m.put("version", f.getVersion()); m.put("issuer", f.getIssuer());
        m.put("effectiveDate", f.getEffectiveDate() != null ? f.getEffectiveDate().toString() : null);
        m.put("status", f.getStatus() != null ? f.getStatus().name() : null);
        m.put("ownerId", f.getOwner() != null ? f.getOwner().getId() : null);
        return m;
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthToken j) return j.getUserId();
        return "system";
    }
}