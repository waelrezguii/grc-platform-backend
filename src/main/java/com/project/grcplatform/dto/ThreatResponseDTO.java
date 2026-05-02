package com.project.grcplatform.dto;

import com.project.grcplatform.constant.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ThreatResponseDTO {

    private String id;
    private String name;
    private String description;
    private String motivation;
    private String objectives;
    private ThreatOrigin origin;
    private ThreatTypeDTO type;
    private ThreatFrequency frequency;
    private ThreatSeverity severity;
    private String scenario;
    private String vectors;
    private String aggravatingFactors;
    private ThreatStatus status;
    private String validatedBy;
    private LocalDateTime validatedAt;
    private String lastReviewedBy;
    private LocalDateTime lastReviewedAt;
    private LocalDateTime nextReviewDue;
    private String reviewNotes;
    private List<AssetSummaryDTO> assets;
    private List<VulnerabilitySummaryDTO> vulnerabilities;
    private UserSummaryDTO createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
