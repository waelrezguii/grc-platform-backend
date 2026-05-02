package com.project.grcplatform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GovernanceDocumentRequestDTO {

    @NotBlank
    private String title;

    private String description;

    private String documentTypeId;

    private String version;

    private String fileUrl;

    private Long fileSize;

    private String mimeType;

    private String linkedPolicyId;
}
