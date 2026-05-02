package com.project.grcplatform.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScenarioPriorityConfigResponseDTO {

    private String id;
    private String name;
    private String description;
    private Double likelihoodWeight;
    private Double impactWeight;
    private Double regulatoryExposureWeight;
    private Double cvssWeight;
    private Double alertThreshold;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
