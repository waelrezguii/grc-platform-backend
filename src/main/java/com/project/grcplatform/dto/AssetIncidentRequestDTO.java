package com.project.grcplatform.dto;

import com.project.grcplatform.constant.IncidentSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AssetIncidentRequestDTO {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Severity is required")
    private IncidentSeverity severity;

    @NotNull(message = "Occurred date is required")
    private LocalDateTime occurredAt;

    private LocalDateTime resolvedAt;
}
