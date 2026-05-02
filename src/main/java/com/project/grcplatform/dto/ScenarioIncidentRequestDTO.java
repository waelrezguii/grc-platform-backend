package com.project.grcplatform.dto;

import com.project.grcplatform.constant.IncidentSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScenarioIncidentRequestDTO {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private IncidentSeverity severity;

    @NotNull
    private LocalDateTime occurredAt;

    private LocalDateTime resolvedAt;

    /** Actual impact observed — used to calibrate the scenario (REX). */
    private String actualImpact;

    private String reportedById;
}
