package com.project.grcplatform.mapper;

import com.project.grcplatform.dto.ComplianceRequirementRequestDTO;
import com.project.grcplatform.dto.ComplianceRequirementResponseDTO;
import com.project.grcplatform.dto.UserSummaryDTO;
import com.project.grcplatform.model.ComplianceRequirement;

public class ComplianceRequirementMapper {
    private ComplianceRequirementMapper() {}

    public static ComplianceRequirementResponseDTO toDTO(ComplianceRequirement r) {
        return ComplianceRequirementResponseDTO.builder()
                .id(r.getId()).frameworkId(r.getFramework() != null ? r.getFramework().getId() : null).code(r.getCode())
                .title(r.getTitle()).description(r.getDescription()).category(r.getCategory())
                .mandatory(r.getMandatory()).createdBy(UserSummaryDTO.of(r.getOwner()))
                .createdAt(r.getCreatedAt()).updatedAt(r.getUpdatedAt())
                .build();
    }

    public static ComplianceRequirement toEntity(ComplianceRequirementRequestDTO req) {
        return ComplianceRequirement.builder()
                .code(req.getCode()).title(req.getTitle())
                .description(req.getDescription()).category(req.getCategory())
                .mandatory(req.getMandatory() != null ? req.getMandatory() : true)
                .build();
    }

    public static void updateEntity(ComplianceRequirement r, ComplianceRequirementRequestDTO req) {
        if (req.getCode() != null)        r.setCode(req.getCode());
        if (req.getTitle() != null)       r.setTitle(req.getTitle());
        if (req.getDescription() != null) r.setDescription(req.getDescription());
        if (req.getCategory() != null)    r.setCategory(req.getCategory());
        if (req.getMandatory() != null)   r.setMandatory(req.getMandatory());
    }
}