package com.project.grcplatform.repository;

import com.project.grcplatform.constant.ControlEffectiveness;
import com.project.grcplatform.constant.ControlStatus;
import com.project.grcplatform.model.Control;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ControlRepository extends JpaRepository<Control, String> {

    Optional<Control> findByIdAndDeletedFalse(String id);

    @Query("""
        SELECT DISTINCT c FROM Control c
        LEFT JOIN c.scenarios s
        WHERE c.deleted = false
          AND (:title IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', CAST(:title AS string), '%')))
          AND (:isoReference IS NULL OR LOWER(c.isoReference) LIKE LOWER(CONCAT('%', CAST(:isoReference AS string), '%')))
          AND (:categoryId IS NULL OR c.category.id = CAST(:categoryId AS string))
          AND (:typeId IS NULL OR c.type.id = CAST(:typeId AS string))
          AND (:status IS NULL OR c.status = :status)
          AND (:effectiveness IS NULL OR c.effectiveness = :effectiveness)
          AND (:assetId IS NULL OR c.asset.id = CAST(:assetId AS string))
          AND (:scenarioId IS NULL OR s.id = CAST(:scenarioId AS string))
          AND (:ownerId IS NULL OR c.owner.id = CAST(:ownerId AS string))
    """)
    Page<Control> findAllWithFilters(
            @Param("title")         String title,
            @Param("isoReference")  String isoReference,
            @Param("categoryId")    String categoryId,
            @Param("typeId")        String typeId,
            @Param("status")        ControlStatus status,
            @Param("effectiveness") ControlEffectiveness effectiveness,
            @Param("assetId")       String assetId,
            @Param("scenarioId")    String scenarioId,
            @Param("ownerId")       String ownerId,
            Pageable pageable
    );

    /**
     * Used by NotificationScheduler — find non-retired, non-deleted controls
     * whose nextReviewDate has passed.
     */
    @Query("""
        SELECT c FROM Control c
        WHERE c.deleted = false
          AND c.status <> com.project.grcplatform.constant.ControlStatus.RETIRED
          AND c.nextReviewDate IS NOT NULL
          AND c.nextReviewDate < :today
    """)
    List<Control> findOverdueForReview(@Param("today") LocalDate today);
}
