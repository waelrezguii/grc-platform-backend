package com.project.grcplatform.repository;

import com.project.grcplatform.constant.EvaluationStatus;
import com.project.grcplatform.model.ComplianceEvaluation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ComplianceEvaluationRepository extends JpaRepository<ComplianceEvaluation, String> {

    Optional<ComplianceEvaluation> findByIdAndDeletedFalse(String id);

    @Query("""
        SELECT e FROM ComplianceEvaluation e
        WHERE e.deleted = false
          AND e.archived = :archived
          AND (:title IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', CAST(:title AS string), '%')))
          AND (:frameworkId IS NULL OR e.framework.id = CAST(:frameworkId AS string))
          AND (:status IS NULL OR e.status = :status)
          AND (:evaluatorId IS NULL OR e.evaluator.id = CAST(:evaluatorId AS string))
    """)
    Page<ComplianceEvaluation> findAllWithFilters(
            @Param("title")       String title,
            @Param("frameworkId") String frameworkId,
            @Param("status")      EvaluationStatus status,
            @Param("evaluatorId") String evaluatorId,
            @Param("archived")    boolean archived,
            Pageable pageable
    );

    @Query("""
        SELECT e FROM ComplianceEvaluation e
        WHERE e.status = :status
          AND e.updatedAt < :before
          AND e.deleted = false
          AND e.archived = false
    """)
    List<ComplianceEvaluation> findArchiveCandidates(
            @Param("status") EvaluationStatus status,
            @Param("before") LocalDateTime before
    );
}