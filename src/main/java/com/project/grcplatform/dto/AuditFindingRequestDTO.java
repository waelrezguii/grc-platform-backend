package com.project.grcplatform.dto;

import com.project.grcplatform.constant.FindingSeverity;
import com.project.grcplatform.constant.FindingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AuditFindingRequestDTO {
    @NotBlank private String campaignId;
    @NotBlank private String title;
    private String description;
    @NotNull  private FindingType findingType;
    @NotNull  private FindingSeverity severity;
    private String category;
    private String evidence;
    private String recommendation;
}