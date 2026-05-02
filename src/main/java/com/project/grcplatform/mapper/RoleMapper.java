package com.project.grcplatform.mapper;

import com.project.grcplatform.dto.RoleDTO;
import com.project.grcplatform.model.Permission;
import com.project.grcplatform.model.Role;

import java.util.Set;
import java.util.stream.Collectors;

public class RoleMapper {

    public static RoleDTO toDTO(Role role) {
        if (role == null) return null;

        RoleDTO dto = new RoleDTO();

        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());
        dto.setCreatedAt(role.getCreatedAt());
        dto.setUpdatedAt(role.getUpdatedAt());

        if (role.getPermissions() != null) {
            dto.setPermissionNames(
                    role.getPermissions()
                            .stream()
                            .map(Permission::getName)
                            .collect(Collectors.toSet())
            );
        }

        return dto;
    }

    public static void updateEntity(Role role, RoleDTO dto, Set<Permission> permissions) {

        role.setName(dto.getName());
        role.setDescription(dto.getDescription());
        role.setPermissions(permissions);
    }

    public static Role toEntity(RoleDTO dto, Set<Permission> permissions) {

        return Role.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .permissions(permissions)
                .build();
    }
}