package com.project.grcplatform.repository;

import com.project.grcplatform.constant.ActionStatus;
import com.project.grcplatform.model.TreatmentAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TreatmentActionRepository extends JpaRepository<TreatmentAction, String> {

    Optional<TreatmentAction> findByIdAndDeletedFalse(String id);

    List<TreatmentAction> findByPlan_IdAndDeletedFalseOrderByCreatedAtAsc(String planId);

    List<TreatmentAction> findByPlan_IdAndStatusAndDeletedFalse(String planId, ActionStatus status);

    /**
     * Used by NotificationScheduler — find active actions whose dueDate has passed.
     */
    @Query("""
        SELECT a FROM TreatmentAction a
        WHERE a.deleted = false
          AND a.dueDate IS NOT NULL
          AND a.dueDate < :today
          AND a.status NOT IN (
              com.project.grcplatform.constant.ActionStatus.DONE,
              com.project.grcplatform.constant.ActionStatus.CANCELLED
          )
    """)
    List<TreatmentAction> findOverdueActions(@Param("today") LocalDate today);
}