package com.project.grcplatform.mapper;

import com.project.grcplatform.dto.*;
import com.project.grcplatform.model.Control;

public class ControlMapper {

    public static ControlResponseDTO toDTO(Control control) {
        if (control == null) return null;

        ControlCategoryDTO categoryDto = control.getCategory() != null
                ? new ControlCategoryDTO(control.getCategory().getId(), control.getCategory().getName(), control.getCategory().getDescription())
                : null;
        ControlTypeDTO typeDto = control.getType() != null
                ? new ControlTypeDTO(control.getType().getId(), control.getType().getName(), control.getType().getDescription())
                : null;

        ControlResponseDTO dto = new ControlResponseDTO();
        dto.setId(control.getId());
        dto.setIsoReference(control.getIsoReference());
        dto.setTitle(control.getTitle());
        dto.setDescription(control.getDescription());
        dto.setCategory(categoryDto);
        dto.setType(typeDto);
        dto.setStatus(control.getStatus());
        dto.setEffectiveness(control.getEffectiveness());
        dto.setFormalism(control.getFormalism());
        dto.setNature(control.getNature());
        dto.setTiming(control.getTiming());
        dto.setTracabilite(control.getTracabilite());
        dto.setConformite4Yeux(control.getConformite4Yeux());
        dto.setPerformance(control.getPerformance());
        dto.setEfficacy(control.getEfficacy());
        dto.setOwner(UserSummaryDTO.of(control.getOwner()));
        dto.setScope(control.getScope());
        dto.setAssetId(control.getAsset() != null ? control.getAsset().getId() : null);
        dto.setScenarios(control.getScenarios().stream().map(ScenarioSummaryDTO::of).toList());
        dto.setValidatedBy(control.getValidatedBy());
        dto.setValidatedAt(control.getValidatedAt());
        dto.setLastReviewDate(control.getLastReviewDate());
        dto.setNextReviewDate(control.getNextReviewDate());
        dto.setImplementationNotes(control.getImplementationNotes());
        dto.setEvidenceUrl(control.getEvidenceUrl());
        dto.setCreatedBy(UserSummaryDTO.of(control.getCreatedBy()));
        dto.setCreatedAt(control.getCreatedAt());
        dto.setUpdatedAt(control.getUpdatedAt());
        return dto;
    }

    public static Control toEntity(ControlRequestDTO request) {
        if (request == null) return null;

        return Control.builder()
                .isoReference(request.getIsoReference())
                .title(request.getTitle())
                .description(request.getDescription())
                .scope(request.getScope())
                .category(null)  // resolved in service
                .type(null)      // resolved in service
                .owner(null)     // resolved in service
                .effectiveness(request.getEffectiveness())
                .formalism(request.getFormalism())
                .nature(request.getNature())
                .timing(request.getTiming())
                .tracabilite(request.getTracabilite())
                .conformite4Yeux(request.getConformite4Yeux())
                .performance(request.getPerformance())
                .nextReviewDate(request.getNextReviewDate())
                .implementationNotes(request.getImplementationNotes())
                .evidenceUrl(request.getEvidenceUrl())
                .build();
    }

    public static void updateEntity(Control control, ControlRequestDTO request) {
        if (request.getIsoReference() != null)        control.setIsoReference(request.getIsoReference());
        if (request.getTitle() != null)               control.setTitle(request.getTitle());
        if (request.getDescription() != null)         control.setDescription(request.getDescription());
        if (request.getScope() != null)               control.setScope(request.getScope());
        if (request.getEffectiveness() != null)       control.setEffectiveness(request.getEffectiveness());
        if (request.getFormalism() != null)           control.setFormalism(request.getFormalism());
        if (request.getNature() != null)              control.setNature(request.getNature());
        if (request.getTiming() != null)              control.setTiming(request.getTiming());
        if (request.getTracabilite() != null)         control.setTracabilite(request.getTracabilite());
        if (request.getConformite4Yeux() != null)     control.setConformite4Yeux(request.getConformite4Yeux());
        if (request.getPerformance() != null)         control.setPerformance(request.getPerformance());
        if (request.getNextReviewDate() != null)      control.setNextReviewDate(request.getNextReviewDate());
        if (request.getImplementationNotes() != null) control.setImplementationNotes(request.getImplementationNotes());
        if (request.getEvidenceUrl() != null)         control.setEvidenceUrl(request.getEvidenceUrl());
        // category, type, owner, scenarios resolved in service
    }
}
