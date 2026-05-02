package com.project.grcplatform.dto;

import com.project.grcplatform.constant.DocumentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GovernanceDocumentResponseDTO {

    private String id;
    private String title;
    private String description;
    private DocumentTypeDTO documentType;
    private String version;
    private String fileUrl;
    private Long fileSize;
    private String mimeType;
    private DocumentStatus status;
    private String linkedPolicyId;
    private String ownerId;
    private LocalDateTime publishedAt;
    private UserSummaryDTO createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
