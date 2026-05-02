package com.project.grcplatform.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuditTypeDTO {
    private String id;
    private String name;
    private String description;
}
