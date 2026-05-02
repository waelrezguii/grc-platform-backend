package com.project.grcplatform.service;

import com.project.grcplatform.constant.*;
import com.project.grcplatform.dto.ReportRequestDTO;
import com.project.grcplatform.dto.ReportResponseDTO;
import com.project.grcplatform.exception.AppException;
import com.project.grcplatform.mapper.RiskReportMapper;
import com.project.grcplatform.model.RiskReport;
import com.project.grcplatform.repository.OrganisationRepository;
import com.project.grcplatform.repository.RiskAssessmentRepository;
import com.project.grcplatform.repository.RiskReportRepository;
import com.project.grcplatform.security.JwtAuthToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RiskReportService {

    private final RiskReportRepository reportRepository;
    private final RiskAssessmentRepository assessmentRepository;
    private final OrganisationRepository organisationRepository;
    private final ReportBuilderService reportBuilderService;
    private final AuditService auditService;

    public Page<ReportResponseDTO> findAll(ReportType type, ReportStatus status,
                                           String assessmentId, String organisationId,
                                           Pageable pageable) {
        return reportRepository
                .findAllWithFilters(type, status, assessmentId, organisationId, pageable)
                .map(RiskReportMapper::toDTO);
    }

    public ReportResponseDTO findById(String id) {
        return RiskReportMapper.toDTO(getOrThrow(id));
    }

    @Transactional
    public ReportResponseDTO generate(ReportRequestDTO request) {
        String currentUserId = getCurrentUserId();
        String title = request.getTitle() != null
                ? request.getTitle()
                : buildAutoTitle(request.getType());

        RiskReport report = RiskReport.builder()
                .title(title)
                .type(request.getType())
                .status(ReportStatus.GENERATING)
                .generatedBy(currentUserId)
                .build();
        if (request.getAssessmentId() != null) assessmentRepository.findById(request.getAssessmentId()).ifPresent(report::setAssessment);
        if (request.getOrganisationId() != null) organisationRepository.findById(request.getOrganisationId()).ifPresent(report::setOrganisation);
        report = reportRepository.save(report);

        try {
            Map<String, Object> payload = reportBuilderService.build(
                    request.getType(),
                    request.getAssessmentId(),
                    request.getOrganisationId()
            );
            report.setPayload(payload);
            report.setStatus(ReportStatus.READY);
            report.setGeneratedAt(LocalDateTime.now());

            auditService.log(
                    AuditAction.REPORT_GENERATED,
                    AuditEntityType.REPORT,
                    report.getId(),
                    "Report generated: " + title + " [" + request.getType() + "]"
            );

        } catch (Exception e) {
            log.error("Report generation failed for type {}: {}", request.getType(), e.getMessage(), e);
            report.setStatus(ReportStatus.FAILED);
            report.setErrorMessage(e.getMessage());

            auditService.logFailure(
                    AuditAction.REPORT_GENERATED,
                    AuditEntityType.REPORT,
                    report.getId(),
                    "Report generation failed: " + title + " [" + request.getType() + "]",
                    e.getMessage()
            );
        }

        return RiskReportMapper.toDTO(reportRepository.save(report));
    }

    @Transactional
    public ReportResponseDTO regenerate(String id) {
        RiskReport report = getOrThrow(id);
        report.setStatus(ReportStatus.GENERATING);
        report.setErrorMessage(null);
        reportRepository.save(report);

        try {
            Map<String, Object> payload = reportBuilderService.build(
                    report.getType(),
                    report.getAssessment() != null ? report.getAssessment().getId() : null,
                    report.getOrganisation() != null ? report.getOrganisation().getId() : null
            );
            report.setPayload(payload);
            report.setStatus(ReportStatus.READY);
            report.setGeneratedAt(LocalDateTime.now());

            auditService.log(
                    AuditAction.REPORT_REGENERATED,
                    AuditEntityType.REPORT,
                    id,
                    "Report regenerated: " + report.getTitle() + " [" + report.getType() + "]"
            );

        } catch (Exception e) {
            log.error("Report regeneration failed for {}: {}", id.replaceAll("[\r\n]", "_"), e.getMessage(), e);
            report.setStatus(ReportStatus.FAILED);
            report.setErrorMessage(e.getMessage());

            auditService.logFailure(
                    AuditAction.REPORT_REGENERATED,
                    AuditEntityType.REPORT,
                    id,
                    "Report regeneration failed: " + report.getTitle(),
                    e.getMessage()
            );
        }

        return RiskReportMapper.toDTO(reportRepository.save(report));
    }

    @Transactional
    public void delete(String id) {
        RiskReport report = getOrThrow(id);
        report.setDeleted(true);
        reportRepository.save(report);

        auditService.log(
                AuditAction.REPORT_DELETED,
                AuditEntityType.REPORT,
                id,
                "Report deleted: " + report.getTitle() + " [" + report.getType() + "]"
        );
    }

    // -------------------------

    private RiskReport getOrThrow(String id) {
        return reportRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));
    }

    private String buildAutoTitle(ReportType type) {
        String date = LocalDateTime.now().toLocalDate().toString();
        return switch (type) {
            case ASSESSMENT_SUMMARY    -> "Assessment Summary — " + date;
            case RISK_MATRIX           -> "Risk Matrix — " + date;
            case TREATMENT_STATUS      -> "Treatment Status — " + date;
            case CONTROL_EFFECTIVENESS -> "Control Effectiveness — " + date;
            case VULNERABILITY_SUMMARY -> "Vulnerability Summary — " + date;
            case EXECUTIVE_SUMMARY     -> "Executive Summary — " + date;
        };
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthToken jwtAuthToken) {
            return jwtAuthToken.getUserId();
        }
        return "system";
    }
}