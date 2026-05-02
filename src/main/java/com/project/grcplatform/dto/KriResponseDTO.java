package com.project.grcplatform.dto;

import com.project.grcplatform.constant.KriLinkedEntityType;
import com.project.grcplatform.constant.KriStatus;
import com.project.grcplatform.constant.ThresholdDirection;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class KriResponseDTO {

    private String id;
    private String name;
    private String description;
    private KriCategoryDTO category;
    private KriLinkedEntityType linkedEntityType;
    private String linkedEntityId;
    private String unit;
    private Double currentValue;
    private Double warningThreshold;
    private Double breachThreshold;
    private ThresholdDirection thresholdDirection;
    private KriStatus status;
    private LocalDateTime lastEvaluatedAt;
    private UserSummaryDTO createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}