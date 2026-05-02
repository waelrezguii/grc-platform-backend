package com.project.grcplatform.repository;

import com.project.grcplatform.constant.ScenarioStatus;
import com.project.grcplatform.constant.TreatmentStrategy;
import com.project.grcplatform.model.RiskScenario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RiskScenarioRepository extends JpaRepository<RiskScenario, String> {

    Optional<RiskScenario> findByIdAndDeletedFalse(String id);

    @Query("""
        SELECT s FROM RiskScenario s
        WHERE s.deleted = false
          AND s.archived = :archived
          AND (:name IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%')))
          AND (:assetId IS NULL OR s.asset.id = CAST(:assetId AS string))
          AND (:threatId IS NULL OR s.threat.id = CAST(:threatId AS string))
          AND (:status IS NULL OR s.status = :status)
    """)
    Page<RiskScenario> findAllWithFilters(
            @Param("name") String name,
            @Param("assetId") String assetId,
            @Param("threatId") String threatId,
            @Param("status") ScenarioStatus status,
            @Param("archived") boolean archived,
            Pageable pageable
    );
    @Query("""
    SELECT DISTINCT s FROM RiskScenario s
    LEFT JOIN s.asset a
    LEFT JOIN s.threat t
    LEFT JOIN TreatmentPlan p ON p.scenario.id = s.id AND p.deleted = false
                              AND p.status <> com.project.grcplatform.constant.TreatmentPlanStatus.CANCELLED
    WHERE s.deleted = false
      AND s.status = com.project.grcplatform.constant.ScenarioStatus.VALIDATED
      AND (:assetId           IS NULL OR a.id = CAST(:assetId AS string))
      AND (:threatId          IS NULL OR t.id = CAST(:threatId AS string))
      AND (:treatmentStrategy IS NULL OR p.treatmentStrategy = :treatmentStrategy)
""")
    Page<RiskScenario> findForRegister(
            @Param("assetId")           String assetId,
            @Param("threatId")          String threatId,
            @Param("rawRiskLevel")      String rawRiskLevel,      // handled in-memory (score range)
            @Param("globalStatus")      String globalStatus,      // handled in-memory (computed field)
            @Param("treatmentStrategy") TreatmentStrategy treatmentStrategy,
            Pageable pageable
    );
    @Query("""
    SELECT s FROM RiskScenario s
    WHERE s.deleted = false
      AND s.status = com.project.grcplatform.constant.ScenarioStatus.VALIDATED
""")
    List<RiskScenario> findAllValidatedForRegister();

    @Query("""
        SELECT s FROM RiskScenario s
        WHERE s.status = :status
          AND s.updatedAt < :before
          AND s.deleted = false
          AND s.archived = false
    """)
    List<RiskScenario> findArchiveCandidates(
            @Param("status") ScenarioStatus status,
            @Param("before") LocalDateTime before
    );
}