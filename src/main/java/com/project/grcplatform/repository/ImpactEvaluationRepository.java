package com.project.grcplatform.repository;

import com.project.grcplatform.model.ImpactEvaluation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ImpactEvaluationRepository extends JpaRepository<ImpactEvaluation, String> {

    Optional<ImpactEvaluation> findByIdAndDeletedFalse(String id);

    @Query("""
        SELECT e FROM ImpactEvaluation e
        WHERE e.deleted = false
          AND (:vulnerabilityId IS NULL OR e.vulnerability.id = CAST(:vulnerabilityId AS string))
          AND (:scenarioId IS NULL OR e.scenario.id = CAST(:scenarioId AS string))
    """)
    Page<ImpactEvaluation> findAllWithFilters(
            @Param("vulnerabilityId") String vulnerabilityId,
            @Param("scenarioId") String scenarioId,
            Pageable pageable
    );
}
