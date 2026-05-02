package com.project.grcplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class AuditCampaignRequestDTO {
    @NotBlank private String title;
    private String description;
    private String auditTypeId;
    private String scope;
    private String auditorId;   // FK → User
    private String auditeeId;   // FK → User
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
}