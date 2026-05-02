package com.project.grcplatform.repository;

import com.project.grcplatform.constant.ReportStatus;
import com.project.grcplatform.constant.ReportType;
import com.project.grcplatform.model.RiskReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RiskReportRepository extends JpaRepository<RiskReport, String> {

    Optional<RiskReport> findByIdAndDeletedFalse(String id);

    @Query("""
        SELECT r FROM RiskReport r
        WHERE r.deleted = false
          AND (:type IS NULL OR r.type = :type)
          AND (:status IS NULL OR r.status = :status)
          AND (:assessmentId IS NULL OR r.assessment.id = CAST(:assessmentId AS string))
          AND (:organisationId IS NULL OR r.organisation.id = CAST(:organisationId AS string))
    """)
    Page<RiskReport> findAllWithFilters(
            @Param("type") ReportType type,
            @Param("status") ReportStatus status,
            @Param("assessmentId") String assessmentId,
            @Param("organisationId") String organisationId,
            Pageable pageable
    );
}