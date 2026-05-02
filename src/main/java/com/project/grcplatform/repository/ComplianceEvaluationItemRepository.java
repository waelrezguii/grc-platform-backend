package com.project.grcplatform.repository;

import com.project.grcplatform.model.ComplianceEvaluationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ComplianceEvaluationItemRepository extends JpaRepository<ComplianceEvaluationItem, String> {

    Optional<ComplianceEvaluationItem> findByIdAndDeletedFalse(String id);

    List<ComplianceEvaluationItem> findByEvaluation_IdAndDeletedFalse(String evaluationId);

    @Query("""
        SELECT i.score FROM ComplianceEvaluationItem i
        WHERE i.evaluation.id = :evaluationId
          AND i.deleted = false
          AND i.score IS NOT NULL
    """)
    List<Double> findScoresByEvaluationId(@Param("evaluationId") String evaluationId);
}