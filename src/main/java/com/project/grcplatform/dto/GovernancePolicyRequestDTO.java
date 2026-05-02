package com.project.grcplatform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class GovernancePolicyRequestDTO {

    @NotBlank
    private String title;

    private String description;

    private String policyTypeId;

    private String version;

    private LocalDate effectiveDate;

    private LocalDate expiryDate;
}
