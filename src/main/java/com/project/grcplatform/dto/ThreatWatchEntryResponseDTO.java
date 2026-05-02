package com.project.grcplatform.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ThreatWatchEntryResponseDTO {
    private String id;
    private String sourceId;
    private String sourceName;
    private String title;
    private String summary;
    private String entryUrl;
    private LocalDateTime publishedAt;
    private Boolean processed;
    private String processedBy;
    private LocalDateTime processedAt;
    /** Set when the analyst links this entry to an existing threat. */
    private String linkedThreatId;
    private String linkedThreatName;
    private LocalDateTime createdAt;
}
