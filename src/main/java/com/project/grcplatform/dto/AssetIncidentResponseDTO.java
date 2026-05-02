package com.project.grcplatform.dto;

import com.project.grcplatform.constant.IncidentSeverity;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AssetIncidentResponseDTO {
    private String id;
    private String assetId;
    private String assetName;
    private String title;
    private String description;
    private IncidentSeverity severity;
    private LocalDateTime occurredAt;
    private LocalDateTime resolvedAt;
    private UserSummaryDTO reportedBy;
    private LocalDateTime createdAt;
}
