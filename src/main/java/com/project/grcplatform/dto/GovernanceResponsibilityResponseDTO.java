package com.project.grcplatform.dto;

import com.project.grcplatform.constant.ResponsibilityStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class GovernanceResponsibilityResponseDTO {

    private String id;
    private String title;
    private String description;
    private ResponsibilityTypeDTO responsibilityType;
    private String assigneeId;
    private String scope;
    private LocalDate startDate;
    private LocalDate endDate;
    private ResponsibilityStatus status;
    private String ownerId;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
