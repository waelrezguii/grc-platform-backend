package com.project.grcplatform.dto;

import com.project.grcplatform.constant.KriLinkedEntityType;
import com.project.grcplatform.constant.ThresholdDirection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KriRequestDTO {

    @NotBlank
    private String name;

    private String description;

    private String categoryId;

    private KriLinkedEntityType linkedEntityType;
    private String linkedEntityId;

    private String unit;

    private Double currentValue;

    @NotNull
    private Double warningThreshold;

    @NotNull
    private Double breachThreshold;

    @NotNull
    private ThresholdDirection thresholdDirection;
}