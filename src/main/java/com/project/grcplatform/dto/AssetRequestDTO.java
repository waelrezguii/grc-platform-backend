package com.project.grcplatform.dto;

import com.project.grcplatform.constant.LifecycleStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AssetRequestDTO {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotNull(message = "Category is required")
    private String categoryId;

    @NotNull(message = "Type is required")
    private String typeId;

    @NotNull(message = "Business owner is required")
    private String ownerId;

    private String directionCentraleId;
    private String directionId;

    // Optional at DTO level — service enforces per-type rules
    @Min(1) @Max(5)
    private Short confidentiality;

    @Min(1) @Max(5)
    private Short integrity;

    @NotNull(message = "Availability is required")
    @Min(1) @Max(5)
    private Short availability;

    private LifecycleStatus lifecycleStatus;

    private LocalDate acquisitionDate;
    private LocalDate endOfLifeDate;
    private LocalDate warrantyExpiryDate;

    private String ipAddress;
    private String hostname;
}
