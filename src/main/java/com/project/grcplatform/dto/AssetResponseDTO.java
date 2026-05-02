package com.project.grcplatform.dto;

import com.project.grcplatform.constant.LifecycleStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AssetResponseDTO {

    private String id;
    private String ref;
    private String name;
    private String description;
    private AssetCategoryDTO category;
    private AssetTypeDTO type;
    private UserSummaryDTO owner;
    private OrganisationDTO directionCentrale;
    private OrganisationDTO direction;
    private Short confidentiality;
    private Short integrity;
    private Short availability;
    private Short criticalityScore;
    private LifecycleStatus lifecycleStatus;
    private LocalDate acquisitionDate;
    private LocalDate endOfLifeDate;
    private LocalDate warrantyExpiryDate;
    private String ipAddress;
    private String hostname;
    private UserSummaryDTO createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
