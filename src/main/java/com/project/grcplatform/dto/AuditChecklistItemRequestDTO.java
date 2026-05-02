package com.project.grcplatform.dto;

import com.project.grcplatform.constant.ChecklistResult;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuditChecklistItemRequestDTO {
    private String category;
    @NotBlank private String question;
    private String expectedEvidence;
    private ChecklistResult result;
    private String actualEvidence;
    private String comment;
}