package com.project.grcplatform.mapper;

import com.project.grcplatform.constant.LifecycleStatus;
import com.project.grcplatform.dto.AssetCategoryDTO;
import com.project.grcplatform.dto.AssetRequestDTO;
import com.project.grcplatform.dto.AssetResponseDTO;
import com.project.grcplatform.dto.AssetTypeDTO;
import com.project.grcplatform.dto.UserSummaryDTO;
import com.project.grcplatform.model.Asset;

public class AssetMapper {

    public static AssetResponseDTO toDTO(Asset asset) {
        if (asset == null) return null;

        AssetResponseDTO dto = new AssetResponseDTO();
        dto.setId(asset.getId());
        dto.setRef(asset.getRef());
        dto.setName(asset.getName());
        dto.setDescription(asset.getDescription());
        dto.setConfidentiality(asset.getConfidentiality());
        dto.setIntegrity(asset.getIntegrity());
        dto.setAvailability(asset.getAvailability());
        dto.setCriticalityScore(asset.getCriticalityScore());
        dto.setLifecycleStatus(asset.getLifecycleStatus());
        dto.setAcquisitionDate(asset.getAcquisitionDate());
        dto.setEndOfLifeDate(asset.getEndOfLifeDate());
        dto.setWarrantyExpiryDate(asset.getWarrantyExpiryDate());
        dto.setIpAddress(asset.getIpAddress());
        dto.setHostname(asset.getHostname());
        dto.setCreatedBy(UserSummaryDTO.of(asset.getCreatedBy()));
        dto.setCreatedAt(asset.getCreatedAt());
        dto.setUpdatedAt(asset.getUpdatedAt());

        if (asset.getCategory() != null)
            dto.setCategory(new AssetCategoryDTO(
                    asset.getCategory().getId(),
                    asset.getCategory().getName(),
                    asset.getCategory().getDescription()));

        if (asset.getType() != null)
            dto.setType(new AssetTypeDTO(
                    asset.getType().getId(),
                    asset.getType().getName(),
                    asset.getType().getDescription()));

        dto.setOwner(UserSummaryDTO.of(asset.getOwner()));

        if (asset.getDirectionCentrale() != null)
            dto.setDirectionCentrale(OrganisationMapper.toDTO(asset.getDirectionCentrale()));

        if (asset.getDirection() != null)
            dto.setDirection(OrganisationMapper.toDTO(asset.getDirection()));

        return dto;
    }

    public static Asset toEntity(AssetRequestDTO request) {
        if (request == null) return null;

        return Asset.builder()
                .name(request.getName())
                .description(request.getDescription())
                .confidentiality(request.getConfidentiality())
                .integrity(request.getIntegrity())
                .availability(request.getAvailability())
                .lifecycleStatus(request.getLifecycleStatus() != null
                        ? request.getLifecycleStatus()
                        : LifecycleStatus.ACTIVE)
                .ipAddress(request.getIpAddress())
                .hostname(request.getHostname())
                .build();
    }

    public static void updateEntity(Asset asset, AssetRequestDTO request) {
        if (request.getName()            != null) asset.setName(request.getName());
        if (request.getDescription()     != null) asset.setDescription(request.getDescription());
        if (request.getConfidentiality() != null) asset.setConfidentiality(request.getConfidentiality());
        if (request.getIntegrity()       != null) asset.setIntegrity(request.getIntegrity());
        if (request.getAvailability()    != null) asset.setAvailability(request.getAvailability());
        if (request.getLifecycleStatus()    != null) asset.setLifecycleStatus(request.getLifecycleStatus());
        if (request.getAcquisitionDate()    != null) asset.setAcquisitionDate(request.getAcquisitionDate());
        if (request.getEndOfLifeDate()      != null) asset.setEndOfLifeDate(request.getEndOfLifeDate());
        if (request.getWarrantyExpiryDate() != null) asset.setWarrantyExpiryDate(request.getWarrantyExpiryDate());
        if (request.getIpAddress()          != null) asset.setIpAddress(request.getIpAddress());
        if (request.getHostname()           != null) asset.setHostname(request.getHostname());
    }
}
