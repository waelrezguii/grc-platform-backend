package com.project.grcplatform.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class RoleDTO {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Set<String> permissionNames;
}