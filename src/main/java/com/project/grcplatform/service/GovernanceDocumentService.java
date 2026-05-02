package com.project.grcplatform.service;

import com.project.grcplatform.constant.*;
import com.project.grcplatform.dto.GovernanceDocumentRequestDTO;
import com.project.grcplatform.dto.GovernanceDocumentResponseDTO;
import com.project.grcplatform.exception.AppException;
import com.project.grcplatform.exception.NotFoundException;
import com.project.grcplatform.mapper.GovernanceDocumentMapper;
import com.project.grcplatform.model.DocumentType;
import com.project.grcplatform.model.GovernanceDocument;
import com.project.grcplatform.model.GovernanceDocumentHistory;
import com.project.grcplatform.repository.DocumentTypeRepository;
import com.project.grcplatform.repository.GovernanceDocumentHistoryRepository;
import com.project.grcplatform.repository.GovernanceDocumentRepository;
import com.project.grcplatform.repository.UserRepository;
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
public class GovernanceDocumentService {

    private final GovernanceDocumentRepository documentRepository;
    private final GovernanceDocumentHistoryRepository historyRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    // ─── Read ────────────────────────────────────────────────────────────────────

    public Page<GovernanceDocumentResponseDTO> findAll(String title, String documentTypeId,
                                                       DocumentStatus status, String linkedPolicyId,
                                                       Pageable pageable) {
        return documentRepository.findAllWithFilters(title, documentTypeId, status, linkedPolicyId, pageable)
                .map(GovernanceDocumentMapper::toDTO);
    }

    public GovernanceDocumentResponseDTO findById(String id) {
        return GovernanceDocumentMapper.toDTO(getOrThrow(id));
    }

    public List<GovernanceDocumentHistory> getHistory(String id) {
        getOrThrow(id);
        return historyRepository.findByDocument_IdOrderByCreatedAtDesc(id);
    }

    // ─── Write ───────────────────────────────────────────────────────────────────

    @Transactional
    public GovernanceDocumentResponseDTO create(GovernanceDocumentRequestDTO request) {
        GovernanceDocument doc = GovernanceDocumentMapper.toEntity(request);
        String currentUserId = getCurrentUserId();
        userRepository.findById(currentUserId).ifPresent(doc::setOwner);

        if (request.getDocumentTypeId() != null) {
            DocumentType documentType = documentTypeRepository.findById(request.getDocumentTypeId())
                    .orElseThrow(() -> new NotFoundException("Document type not found: " + request.getDocumentTypeId()));
            doc.setDocumentType(documentType);
        }

        GovernanceDocument saved = documentRepository.saveAndFlush(doc);
        GovernanceDocument fresh = documentRepository.findByIdAndDeletedFalse(saved.getId()).orElse(saved);
        saveHistory(fresh, "Document created");

        auditService.log(AuditAction.GOVERNANCE_DOCUMENT_CREATED, AuditEntityType.GOVERNANCE_DOCUMENT,
                fresh.getId(), "Document created: " + fresh.getTitle());

        return GovernanceDocumentMapper.toDTO(fresh);
    }

    @Transactional
    public GovernanceDocumentResponseDTO update(String id, GovernanceDocumentRequestDTO request) {
        GovernanceDocument doc = getOrThrow(id);
        if (doc.getStatus() == DocumentStatus.ARCHIVED || doc.getStatus() == DocumentStatus.OBSOLETE) {
            throw new AppException(ErrorCode.GOVERNANCE_DOCUMENT_INVALID_TRANSITION);
        }
        Map<String, Object> old = snapshot(doc);
        GovernanceDocumentMapper.updateEntity(doc, request);

        if (request.getDocumentTypeId() != null) {
            DocumentType documentType = documentTypeRepository.findById(request.getDocumentTypeId())
                    .orElseThrow(() -> new NotFoundException("Document type not found: " + request.getDocumentTypeId()));
            doc.setDocumentType(documentType);
        }

        GovernanceDocument saved = documentRepository.save(doc);
        saveHistory(saved, "Document updated");

        auditService.log(AuditAction.GOVERNANCE_DOCUMENT_UPDATED, AuditEntityType.GOVERNANCE_DOCUMENT,
                saved.getId(), "Document updated: " + saved.getTitle(), old, snapshot(saved));

        return GovernanceDocumentMapper.toDTO(saved);
    }

    @Transactional
    public GovernanceDocumentResponseDTO publish(String id) {
        GovernanceDocument doc = getOrThrow(id);
        if (doc.getStatus() != DocumentStatus.DRAFT) {
            throw new AppException(ErrorCode.GOVERNANCE_DOCUMENT_INVALID_TRANSITION);
        }
        doc.setStatus(DocumentStatus.PUBLISHED);
        doc.setPublishedAt(LocalDateTime.now());
        GovernanceDocument saved = documentRepository.save(doc);
        saveHistory(saved, "Document published");

        auditService.log(AuditAction.GOVERNANCE_DOCUMENT_PUBLISHED, AuditEntityType.GOVERNANCE_DOCUMENT,
                saved.getId(), "Document published: " + saved.getTitle());

        return GovernanceDocumentMapper.toDTO(saved);
    }

    @Transactional
    public GovernanceDocumentResponseDTO archive(String id) {
        GovernanceDocument doc = getOrThrow(id);
        if (doc.getStatus() != DocumentStatus.PUBLISHED) {
            throw new AppException(ErrorCode.GOVERNANCE_DOCUMENT_INVALID_TRANSITION);
        }
        doc.setStatus(DocumentStatus.ARCHIVED);
        GovernanceDocument saved = documentRepository.save(doc);
        saveHistory(saved, "Document archived");

        auditService.log(AuditAction.GOVERNANCE_DOCUMENT_ARCHIVED, AuditEntityType.GOVERNANCE_DOCUMENT,
                saved.getId(), "Document archived: " + saved.getTitle());

        return GovernanceDocumentMapper.toDTO(saved);
    }

    @Transactional
    public GovernanceDocumentResponseDTO markObsolete(String id) {
        GovernanceDocument doc = getOrThrow(id);
        if (doc.getStatus() == DocumentStatus.OBSOLETE) {
            throw new AppException(ErrorCode.GOVERNANCE_DOCUMENT_INVALID_TRANSITION);
        }
        doc.setStatus(DocumentStatus.OBSOLETE);
        GovernanceDocument saved = documentRepository.save(doc);
        saveHistory(saved, "Document marked obsolete");

        auditService.log(AuditAction.GOVERNANCE_DOCUMENT_OBSOLETE, AuditEntityType.GOVERNANCE_DOCUMENT,
                saved.getId(), "Document obsolete: " + saved.getTitle());

        return GovernanceDocumentMapper.toDTO(saved);
    }

    @Transactional
    public void delete(String id) {
        GovernanceDocument doc = getOrThrow(id);
        doc.setDeleted(true);
        documentRepository.save(doc);
        saveHistory(doc, "Document deleted");

        auditService.log(AuditAction.GOVERNANCE_DOCUMENT_DELETED, AuditEntityType.GOVERNANCE_DOCUMENT,
                id, "Document deleted: " + doc.getTitle());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private GovernanceDocument getOrThrow(String id) {
        return documentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.GOVERNANCE_DOCUMENT_NOT_FOUND));
    }

    private void saveHistory(GovernanceDocument doc, String summary) {
        GovernanceDocumentHistory h = GovernanceDocumentHistory.builder()
                .document(doc)
                .changeSummary(summary)
                .snapshot(snapshot(doc))
                .build();
        historyRepository.save(h);
    }

    private Map<String, Object> snapshot(GovernanceDocument doc) {
        Map<String, Object> map = new HashMap<>();
        map.put("id",             doc.getId());
        map.put("title",          doc.getTitle());
        map.put("description",    doc.getDescription());
        map.put("documentType",   doc.getDocumentType()  != null ? doc.getDocumentType().getName()  : null);
        map.put("version",        doc.getVersion());
        map.put("fileUrl",        doc.getFileUrl());
        map.put("fileSize",       doc.getFileSize());
        map.put("mimeType",       doc.getMimeType());
        map.put("status",         doc.getStatus()        != null ? doc.getStatus().name()        : null);
        map.put("linkedPolicyId", doc.getLinkedPolicy() != null ? doc.getLinkedPolicy().getId() : null);
        map.put("ownerId",        doc.getOwner() != null ? doc.getOwner().getId() : null);
        map.put("publishedAt",    doc.getPublishedAt()   != null ? doc.getPublishedAt().toString() : null);
        return map;
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthToken jwtAuthToken) return jwtAuthToken.getUserId();
        return "system";
    }
}
