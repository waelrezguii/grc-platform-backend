package com.project.grcplatform.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrganisationDTO {
    private String id;
    private String code;
    private String name;
    private String description;
    private String phoneNumber;
    private Integer sortOrder;
    private String level;
    private String parentId;
    private String parentName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}