package com.project.grcplatform.dto;

import lombok.Data;

@Data
public class UserGradeDTO {
    private Long id;
    private String name;
    private String description;
    private Integer orderLevel;
}