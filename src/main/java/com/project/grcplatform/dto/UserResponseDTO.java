package com.project.grcplatform.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.Set;

@Data
@Builder
public class UserResponseDTO {

    private String id;
    private String email;
    private String firstname;
    private String lastname;
    private String phoneNumber;
    private boolean isActive;
    private boolean mustChangePassword;
    private boolean mfaEnabled;
    private String role;
    private String organisationName;
    private String gradeName;
    private Set<String> permissions;
    private Map<String, Object> preferences;
    private String lastLogin;
    private String createdAt;
    private String updatedAt;
}