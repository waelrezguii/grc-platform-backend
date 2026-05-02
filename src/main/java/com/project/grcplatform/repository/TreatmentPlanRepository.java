package com.project.grcplatform.repository;

import com.project.grcplatform.constant.TreatmentPlanStatus;
import com.project.grcplatform.constant.TreatmentStrategy;
import com.project.grcplatform.model.TreatmentPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TreatmentPlanRepository extends JpaRepository<TreatmentPlan, String> {

    Optional<TreatmentPlan> findByIdAndDeletedFalse(String id);

    @Query("""
        SELECT p FROM TreatmentPlan p
        WHERE p.deleted = false
          AND p.archived = :archived
          AND (:title IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', CAST(:title AS string), '%')))
          AND (:scenarioId IS NULL OR p.scenario.id = CAST(:scenarioId AS string))
          AND (:assessmentId IS NULL OR p.assessment.id = CAST(:assessmentId AS string))
          AND (:ownerId IS NULL OR p.createdBy.id = CAST(:ownerId AS string))
          AND (:status IS NULL OR p.status = :status)
          AND (:treatmentStrategy IS NULL OR p.treatmentStrategy = :treatmentStrategy)
    """)
    Page<TreatmentPlan> findAllWithFilters(
            @Param("title") String title,
            @Param("scenarioId") String scenarioId,
            @Param("assessmentId") String assessmentId,
            @Param("ownerId") String ownerId,
            @Param("status") TreatmentPlanStatus status,
            @Param("treatmentStrategy") TreatmentStrategy treatmentStrategy,
            @Param("archived") boolean archived,
            Pageable pageable
    );
    @Query("""
    SELECT p FROM TreatmentPlan p
    WHERE p.scenario.id = :scenarioId
      AND p.deleted = false
      AND p.status <> com.project.grcplatform.constant.TreatmentPlanStatus.CANCELLED
    ORDER BY
        CASE p.status
            WHEN 'COMPLETED'    THEN 1
            WHEN 'IN_PROGRESS'  THEN 2
            WHEN 'APPROVED'     THEN 3
            WHEN 'DRAFT'        THEN 4
            ELSE 5
        END ASC,
        p.createdAt DESC
""")
    List<TreatmentPlan> findLatestByScenarioId(@Param("scenarioId") String scenarioId);
    @Query("""
    SELECT p FROM TreatmentPlan p
    WHERE p.deleted = false
      AND p.status <> com.project.grcplatform.constant.TreatmentPlanStatus.CANCELLED
""")
    List<TreatmentPlan> findAllActiveForRegister();

    @Query("""
        SELECT p FROM TreatmentPlan p
        WHERE p.status = :status
          AND p.updatedAt < :before
          AND p.deleted = false
          AND p.archived = false
    """)
    List<TreatmentPlan> findArchiveCandidates(
            @Param("status") TreatmentPlanStatus status,
            @Param("before") LocalDateTime before
    );
}