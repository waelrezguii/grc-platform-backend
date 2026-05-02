package com.project.grcplatform.mapper;

import com.project.grcplatform.dto.KriCategoryDTO;
import com.project.grcplatform.dto.KriRequestDTO;
import com.project.grcplatform.dto.KriResponseDTO;
import com.project.grcplatform.dto.UserSummaryDTO;
import com.project.grcplatform.model.Kri;
import com.project.grcplatform.model.KriCategory;

public class KriMapper {

    private KriMapper() {}

    public static KriResponseDTO toDTO(Kri kri) {
        return KriResponseDTO.builder()
                .id(kri.getId())
                .name(kri.getName())
                .description(kri.getDescription())
                .category(kri.getCategory() != null ? KriCategoryDTO.builder()
                        .id(kri.getCategory().getId())
                        .name(kri.getCategory().getName())
                        .description(kri.getCategory().getDescription())
                        .build() : null)
                .linkedEntityType(kri.getLinkedEntityType())
                .linkedEntityId(kri.getLinkedEntityId())
                .unit(kri.getUnit())
                .currentValue(kri.getCurrentValue())
                .warningThreshold(kri.getWarningThreshold())
                .breachThreshold(kri.getBreachThreshold())
                .thresholdDirection(kri.getThresholdDirection())
                .status(kri.getStatus())
                .lastEvaluatedAt(kri.getLastEvaluatedAt())
                .createdBy(UserSummaryDTO.of(kri.getCreatedBy()))
                .createdAt(kri.getCreatedAt())
                .updatedAt(kri.getUpdatedAt())
                .build();
    }

    public static Kri toEntity(KriRequestDTO request) {
        return Kri.builder()
                .name(request.getName())
                .description(request.getDescription())
                // category is resolved by service from categoryId
                .category(null)
                .linkedEntityType(request.getLinkedEntityType())
                .linkedEntityId(request.getLinkedEntityId())
                .unit(request.getUnit())
                .currentValue(request.getCurrentValue())
                .warningThreshold(request.getWarningThreshold())
                .breachThreshold(request.getBreachThreshold())
                .thresholdDirection(request.getThresholdDirection())
                .build();
    }

    public static void updateEntity(Kri kri, KriRequestDTO request) {
        if (request.getName() != null)               kri.setName(request.getName());
        if (request.getDescription() != null)        kri.setDescription(request.getDescription());
        // category is resolved by service from categoryId
        if (request.getLinkedEntityType() != null)   kri.setLinkedEntityType(request.getLinkedEntityType());
        if (request.getLinkedEntityId() != null)     kri.setLinkedEntityId(request.getLinkedEntityId());
        if (request.getUnit() != null)               kri.setUnit(request.getUnit());
        if (request.getCurrentValue() != null)       kri.setCurrentValue(request.getCurrentValue());
        if (request.getWarningThreshold() != null)   kri.setWarningThreshold(request.getWarningThreshold());
        if (request.getBreachThreshold() != null)    kri.setBreachThreshold(request.getBreachThreshold());
        if (request.getThresholdDirection() != null) kri.setThresholdDirection(request.getThresholdDirection());
        // status is recalculated automatically in @PreUpdate
    }
}