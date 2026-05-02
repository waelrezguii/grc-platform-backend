package com.project.grcplatform.dto;

import com.project.grcplatform.constant.ReportType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportRequestDTO {

    @NotNull(message = "Report type is required")
    private ReportType type;

    // Optional — scopes report to a specific assessment
    private String assessmentId;

    // Optional — scopes report to a specific organisation
    private String organisationId;

    // Optional custom title — generated automatically if not provided
    private String title;
}