package com.project.grcplatform.dto;

import com.project.grcplatform.constant.ChecklistResult;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class AuditChecklistItemResponseDTO {
    private String id;
    private String campaignId;
    private String campaignTitle;
    private String category;
    private String question;
    private String expectedEvidence;
    private ChecklistResult result;
    private String actualEvidence;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}