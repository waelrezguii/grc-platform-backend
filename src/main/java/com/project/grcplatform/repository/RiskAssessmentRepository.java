package com.project.grcplatform.repository;

import com.project.grcplatform.constant.AssessmentStatus;
import com.project.grcplatform.model.RiskAssessment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, String> {

    Optional<RiskAssessment> findByIdAndDeletedFalse(String id);

    @Query("""
        SELECT a FROM RiskAssessment a
        WHERE a.deleted = false
          AND (:title IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', CAST(:title AS string), '%')))
          AND (:organisationId IS NULL OR a.organisation.id = CAST(:organisationId AS string))
          AND (:status IS NULL OR a.status = :status)
    """)
    Page<RiskAssessment> findAllWithFilters(
            @Param("title")          String title,
            @Param("organisationId") String organisationId,
            @Param("status")         AssessmentStatus status,
            Pageable pageable
    );

    /**
     * Used by NotificationScheduler — find active assessments whose endDate
     * is on or before the given threshold (i.e., due within N days).
     */
    @Query("""
        SELECT a FROM RiskAssessment a
        WHERE a.deleted = false
          AND a.endDate IS NOT NULL
          AND a.endDate <= :threshold
          AND a.status IN :activeStatuses
    """)
    List<RiskAssessment> findDueSoon(
            @Param("threshold")     LocalDate threshold,
            @Param("activeStatuses") List<AssessmentStatus> activeStatuses
    );
    @Query("""
    SELECT a FROM RiskAssessment a
    JOIN RiskAssessmentScenario ras ON ras.assessment.id = a.id
    WHERE ras.scenario.id = :scenarioId
      AND a.deleted = false
    ORDER BY
        CASE a.status
            WHEN 'COMPLETED'    THEN 1
            WHEN 'IN_PROGRESS'  THEN 2
            WHEN 'UNDER_REVIEW' THEN 3
            WHEN 'PLANNED'      THEN 4
            ELSE 5
        END ASC,
        a.createdAt DESC
""")
    Optional<RiskAssessment> findLatestByScenarioId(@Param("scenarioId") String scenarioId);
}