package com.project.grcplatform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class GovernanceResponsibilityRequestDTO {

    @NotBlank
    private String title;

    private String description;

    private String responsibilityTypeId;

    @NotBlank
    private String assigneeId;

    private String scope;

    private LocalDate startDate;

    private LocalDate endDate;
}
