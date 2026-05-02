package com.project.grcplatform.mapper;

import com.project.grcplatform.dto.OrganisationDTO;
import com.project.grcplatform.model.Organisation;

public class OrganisationMapper {

    public static OrganisationDTO toDTO(Organisation org) {
        if (org == null) return null;

        OrganisationDTO dto = new OrganisationDTO();
        dto.setId(org.getId());
        dto.setCode(org.getCode());
        dto.setName(org.getName());
        dto.setDescription(org.getDescription());
        dto.setPhoneNumber(org.getPhoneNumber());
        dto.setSortOrder(org.getSortOrder());
        dto.setLevel(org.getLevel());
        dto.setCreatedAt(org.getCreatedAt());
        dto.setUpdatedAt(org.getUpdatedAt());

        if (org.getParent() != null) {
            dto.setParentId(org.getParent().getId());
            dto.setParentName(org.getParent().getName());
        }

        return dto;
    }

    public static Organisation toEntity(OrganisationDTO dto) {
        if (dto == null) return null;

        Organisation org = new Organisation();
        if (dto.getId() != null) org.setId(dto.getId());
        org.setCode(dto.getCode());
        org.setName(dto.getName());
        org.setDescription(dto.getDescription());
        org.setPhoneNumber(dto.getPhoneNumber());
        org.setSortOrder(dto.getSortOrder());
        org.setLevel(dto.getLevel());
        // parent resolved in service

        return org;
    }
}
