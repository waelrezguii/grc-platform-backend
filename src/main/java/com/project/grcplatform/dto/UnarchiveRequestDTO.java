package com.project.grcplatform.dto;

import com.project.grcplatform.constant.ArchivableEntityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UnarchiveRequestDTO {

    @NotNull
    private ArchivableEntityType entityType;

    @NotBlank
    private String entityId;
}
