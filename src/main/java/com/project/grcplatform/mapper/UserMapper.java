package com.project.grcplatform.mapper;

import com.project.grcplatform.dto.UserResponseDTO;
import com.project.grcplatform.model.Permission;
import com.project.grcplatform.model.User;

import java.util.stream.Collectors;

public class UserMapper {

    public static UserResponseDTO toDTO(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .phoneNumber(user.getPhoneNumber())
                .isActive(user.getIsActive())
                .mustChangePassword(Boolean.TRUE.equals(user.getMustChangePassword()))
                .mfaEnabled(Boolean.TRUE.equals(user.getMfaEnabled()))
                .role(user.getRole() != null ? user.getRole().getName() : null)
                .organisationName(user.getOrganisation() != null ? user.getOrganisation().getName() : null)
                .gradeName(user.getGrade() != null ? user.getGrade().getName() : null)
                .permissions(user.getExtraPermissions().stream().map(Permission::getName).collect(Collectors.toSet()))
                .preferences(user.getPreferences())
                .lastLogin(user.getLastLogin() != null ? user.getLastLogin().toString() : null)
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .updatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null)
                .build();
    }
}