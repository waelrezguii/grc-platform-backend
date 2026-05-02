package com.project.grcplatform.dto;

import com.project.grcplatform.constant.AuditCampaignStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data @Builder
public class AuditReportDTO {

    // Campaign summary
    private String campaignId;
    private String title;
    private AuditTypeDTO auditType;
    private String scope;
    private AuditCampaignStatus status;
    private UserSummaryDTO auditor;
    private UserSummaryDTO auditee;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;
    private LocalDateTime reportGeneratedAt;

    // Checklist statistics
    private int totalChecklistItems;
    private Map<String, Long> checklistResultCounts; // {CONFORMANT: 10, NON_CONFORMANT: 3, ...}
    private Double conformanceRate;                  // % of CONFORMANT items

    // Findings summary
    private int totalFindings;
    private Map<String, Long> findingsBySeverity;    // {CRITICAL: 1, MAJOR: 2, MINOR: 5}
    private Map<String, Long> findingsByType;        // {NON_CONFORMITY: 3, OBSERVATION: 5}
    private List<AuditFindingResponseDTO> findings;

    // Recommendations summary
    private int totalRecommendations;
    private Map<String, Long> recommendationsByStatus;
    private List<AuditRecommendationResponseDTO> recommendations;
}