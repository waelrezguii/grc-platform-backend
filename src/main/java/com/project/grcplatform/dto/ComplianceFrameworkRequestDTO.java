package com.project.grcplatform.dto;

import com.project.grcplatform.constant.FrameworkType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ComplianceFrameworkRequestDTO {
    @NotBlank private String name;
    private String description;
    @NotNull private FrameworkType frameworkType;
    private String version;
    private String issuer;
    private LocalDate effectiveDate;
}