package com.project.grcplatform.dto;

import com.project.grcplatform.constant.ArchivableEntityType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArchiveRuleResponseDTO {

    private String id;
    private ArchivableEntityType entityType;
    private String triggerStatus;
    private int delayDays;
    private String description;
    private boolean enabled;
    private LocalDateTime lastRunAt;
    private Integer lastRunArchived;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
