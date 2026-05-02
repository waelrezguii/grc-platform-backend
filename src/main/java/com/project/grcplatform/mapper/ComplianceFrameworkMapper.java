package com.project.grcplatform.mapper;

import com.project.grcplatform.dto.*;
import com.project.grcplatform.model.*;

public class ComplianceFrameworkMapper {
    private ComplianceFrameworkMapper() {}

    public static ComplianceFrameworkResponseDTO toDTO(ComplianceFramework f) {
        return ComplianceFrameworkResponseDTO.builder()
                .id(f.getId()).name(f.getName()).description(f.getDescription())
                .frameworkType(f.getFrameworkType()).version(f.getVersion())
                .issuer(f.getIssuer()).effectiveDate(f.getEffectiveDate())
                .status(f.getStatus()).ownerId(f.getOwner() != null ? f.getOwner().getId() : null)
                .createdBy(UserSummaryDTO.of(f.getOwner())).createdAt(f.getCreatedAt()).updatedAt(f.getUpdatedAt())
                .build();
    }

    public static ComplianceFramework toEntity(ComplianceFrameworkRequestDTO r) {
        return ComplianceFramework.builder()
                .name(r.getName()).description(r.getDescription())
                .frameworkType(r.getFrameworkType()).version(r.getVersion())
                .issuer(r.getIssuer()).effectiveDate(r.getEffectiveDate())
                .build();
    }

    public static void updateEntity(ComplianceFramework f, ComplianceFrameworkRequestDTO r) {
        if (r.getName() != null)          f.setName(r.getName());
        if (r.getDescription() != null)   f.setDescription(r.getDescription());
        if (r.getFrameworkType() != null) f.setFrameworkType(r.getFrameworkType());
        if (r.getVersion() != null)       f.setVersion(r.getVersion());
        if (r.getIssuer() != null)        f.setIssuer(r.getIssuer());
        if (r.getEffectiveDate() != null) f.setEffectiveDate(r.getEffectiveDate());
    }
}