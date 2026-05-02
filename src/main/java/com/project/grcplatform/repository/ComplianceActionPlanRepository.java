package com.project.grcplatform.repository;

import com.project.grcplatform.constant.ActionPlanPriority;
import com.project.grcplatform.constant.ActionPlanStatus;
import com.project.grcplatform.model.ComplianceActionPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ComplianceActionPlanRepository extends JpaRepository<ComplianceActionPlan, String> {

    Optional<ComplianceActionPlan> findByIdAndDeletedFalse(String id);

    @Query("""
        SELECT p FROM ComplianceActionPlan p
        WHERE p.deleted = false
          AND (:title IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', CAST(:title AS string), '%')))
          AND (:evaluationId IS NULL OR p.evaluation.id = CAST(:evaluationId AS string))
          AND (:status IS NULL OR p.status = :status)
          AND (:assigneeId IS NULL OR p.assignee.id = CAST(:assigneeId AS string))
          AND (:priority IS NULL OR p.priority = :priority)
    """)
    Page<ComplianceActionPlan> findAllWithFilters(
            @Param("title")        String title,
            @Param("evaluationId") String evaluationId,
            @Param("status")       ActionPlanStatus status,
            @Param("assigneeId")   String assigneeId,
            @Param("priority")     ActionPlanPriority priority,
            Pageable pageable
    );

    @Query("""
        SELECT p FROM ComplianceActionPlan p
        WHERE p.deleted = false
          AND p.dueDate < :today
          AND p.status NOT IN (
              com.project.grcplatform.constant.ActionPlanStatus.DONE,
              com.project.grcplatform.constant.ActionPlanStatus.CANCELLED
          )
    """)
    List<ComplianceActionPlan> findOverduePlans(@Param("today") LocalDate today);
}