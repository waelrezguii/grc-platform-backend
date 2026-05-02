package com.project.grcplatform.mapper;

import com.project.grcplatform.dto.DocumentTypeDTO;
import com.project.grcplatform.dto.GovernanceDocumentRequestDTO;
import com.project.grcplatform.dto.GovernanceDocumentResponseDTO;
import com.project.grcplatform.dto.UserSummaryDTO;
import com.project.grcplatform.model.GovernanceDocument;

public class GovernanceDocumentMapper {

    private GovernanceDocumentMapper() {}

    public static GovernanceDocumentResponseDTO toDTO(GovernanceDocument doc) {
        DocumentTypeDTO documentTypeDto = doc.getDocumentType() != null
                ? new DocumentTypeDTO(doc.getDocumentType().getId(), doc.getDocumentType().getName(), doc.getDocumentType().getDescription())
                : null;

        return GovernanceDocumentResponseDTO.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .description(doc.getDescription())
                .documentType(documentTypeDto)
                .version(doc.getVersion())
                .fileUrl(doc.getFileUrl())
                .fileSize(doc.getFileSize())
                .mimeType(doc.getMimeType())
                .status(doc.getStatus())
                .linkedPolicyId(doc.getLinkedPolicy() != null ? doc.getLinkedPolicy().getId() : null)
                .ownerId(doc.getOwner() != null ? doc.getOwner().getId() : null)
                .publishedAt(doc.getPublishedAt())
                .createdBy(UserSummaryDTO.of(doc.getOwner()))
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }

    public static GovernanceDocument toEntity(GovernanceDocumentRequestDTO request) {
        return GovernanceDocument.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .documentType(null)  // resolved in service
                .version(request.getVersion())
                .fileUrl(request.getFileUrl())
                .fileSize(request.getFileSize())
                .mimeType(request.getMimeType())
                .build();
    }

    public static void updateEntity(GovernanceDocument doc, GovernanceDocumentRequestDTO request) {
        if (request.getTitle() != null)          doc.setTitle(request.getTitle());
        if (request.getDescription() != null)    doc.setDescription(request.getDescription());
        if (request.getVersion() != null)        doc.setVersion(request.getVersion());
        if (request.getFileUrl() != null)        doc.setFileUrl(request.getFileUrl());
        if (request.getFileSize() != null)       doc.setFileSize(request.getFileSize());
        if (request.getMimeType() != null)       doc.setMimeType(request.getMimeType());
        // documentType resolved in service
    }
}
