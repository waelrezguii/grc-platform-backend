package com.project.grcplatform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CveImportRequestDTO {
    @NotBlank private String cveId;    // CVE-2021-44228
    @NotBlank private String assetId;  // UUID of the asset to link
}