package com.project.grcplatform.dto;

import com.project.grcplatform.constant.ReportStatus;
import com.project.grcplatform.constant.ReportType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class ReportResponseDTO {

    private String id;
    private String title;
    private ReportType type;
    private ReportStatus status;
    private String assessmentId;
    private String organisationId;
    private Map<String, Object> payload;
    private UserSummaryDTO generatedBy;
    private LocalDateTime generatedAt;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}