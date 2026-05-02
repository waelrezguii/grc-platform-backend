package com.project.grcplatform.mapper;

import com.project.grcplatform.dto.GovernanceResponsibilityRequestDTO;
import com.project.grcplatform.dto.GovernanceResponsibilityResponseDTO;
import com.project.grcplatform.dto.ResponsibilityTypeDTO;
import com.project.grcplatform.model.GovernanceResponsibility;

public class GovernanceResponsibilityMapper {

    private GovernanceResponsibilityMapper() {}

    public static GovernanceResponsibilityResponseDTO toDTO(GovernanceResponsibility r) {
        ResponsibilityTypeDTO responsibilityTypeDto = r.getResponsibilityType() != null
                ? new ResponsibilityTypeDTO(r.getResponsibilityType().getId(), r.getResponsibilityType().getName(), r.getResponsibilityType().getDescription())
                : null;

        return GovernanceResponsibilityResponseDTO.builder()
                .id(r.getId())
                .title(r.getTitle())
                .description(r.getDescription())
                .responsibilityType(responsibilityTypeDto)
                .assigneeId(r.getAssignee() != null ? r.getAssignee().getId() : null)
                .scope(r.getScope())
                .startDate(r.getStartDate())
                .endDate(r.getEndDate())
                .status(r.getStatus())
                .ownerId(r.getOwner() != null ? r.getOwner().getId() : null)
                .createdBy(r.getCreatedBy() != null ? r.getCreatedBy().getId() : null)
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    public static GovernanceResponsibility toEntity(GovernanceResponsibilityRequestDTO request) {
        return GovernanceResponsibility.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .responsibilityType(null)  // resolved in service
                .scope(request.getScope())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();
    }

    public static void updateEntity(GovernanceResponsibility r, GovernanceResponsibilityRequestDTO request) {
        if (request.getTitle() != null)       r.setTitle(request.getTitle());
        if (request.getDescription() != null) r.setDescription(request.getDescription());
        if (request.getScope() != null)       r.setScope(request.getScope());
        if (request.getStartDate() != null)   r.setStartDate(request.getStartDate());
        if (request.getEndDate() != null)     r.setEndDate(request.getEndDate());
        // responsibilityType resolved in service
    }
}
