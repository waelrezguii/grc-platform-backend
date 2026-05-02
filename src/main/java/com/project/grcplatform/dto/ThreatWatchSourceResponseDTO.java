package com.project.grcplatform.dto;

import com.project.grcplatform.constant.WatchSourceType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ThreatWatchSourceResponseDTO {
    private String id;
    private String name;
    private String url;
    private WatchSourceType type;
    private String description;
    private Boolean enabled;
    private LocalDateTime lastFetchAt;
    private Integer lastFetchCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
