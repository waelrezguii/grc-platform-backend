package com.project.grcplatform.dto;

import com.project.grcplatform.constant.IncidentSeverity;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScenarioIncidentResponseDTO {
    private String id;
    private String scenarioId;
    private String scenarioName;
    private String title;
    private String description;
    private IncidentSeverity severity;
    private LocalDateTime occurredAt;
    private LocalDateTime resolvedAt;
    private String actualImpact;
    private UserSummaryDTO reportedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
