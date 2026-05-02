package com.project.grcplatform.mapper;

import com.project.grcplatform.dto.ReportResponseDTO;
import com.project.grcplatform.dto.UserSummaryDTO;
import com.project.grcplatform.model.RiskReport;

public class RiskReportMapper {

    public static ReportResponseDTO toDTO(RiskReport report) {
        if (report == null) return null;

        ReportResponseDTO dto = new ReportResponseDTO();
        dto.setId(report.getId());
        dto.setTitle(report.getTitle());
        dto.setType(report.getType());
        dto.setStatus(report.getStatus());
        dto.setAssessmentId(report.getAssessment() != null ? report.getAssessment().getId() : null);
        dto.setOrganisationId(report.getOrganisation() != null ? report.getOrganisation().getId() : null);
        dto.setPayload(report.getPayload());
        dto.setGeneratedBy(UserSummaryDTO.of(report.getOwner()));
        dto.setGeneratedAt(report.getGeneratedAt());
        dto.setErrorMessage(report.getErrorMessage());
        dto.setCreatedAt(report.getCreatedAt());
        dto.setUpdatedAt(report.getUpdatedAt());
        return dto;
    }
}