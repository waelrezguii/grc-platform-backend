package com.project.grcplatform.dto;

import com.project.grcplatform.constant.FrameworkStatus;
import com.project.grcplatform.constant.FrameworkType;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder
public class ComplianceFrameworkResponseDTO {
    private String id;
    private String name;
    private String description;
    private FrameworkType frameworkType;
    private String version;
    private String issuer;
    private LocalDate effectiveDate;
    private FrameworkStatus status;
    private String ownerId;
    private UserSummaryDTO createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}