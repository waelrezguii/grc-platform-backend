package com.project.grcplatform.dto;

import com.project.grcplatform.constant.PolicyStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class GovernancePolicyResponseDTO {

    private String id;
    private String title;
    private String description;
    private PolicyTypeDTO policyType;
    private PolicyStatus status;
    private String version;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private UserSummaryDTO createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
