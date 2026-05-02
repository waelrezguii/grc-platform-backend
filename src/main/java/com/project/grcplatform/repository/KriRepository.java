package com.project.grcplatform.repository;

import com.project.grcplatform.constant.KriLinkedEntityType;
import com.project.grcplatform.constant.KriStatus;
import com.project.grcplatform.model.Kri;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KriRepository extends JpaRepository<Kri, String> {

    Optional<Kri> findByIdAndDeletedFalse(String id);

    @Query("""
        SELECT k FROM Kri k
        WHERE k.deleted = false
          AND (:name IS NULL OR LOWER(k.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%')))
          AND (:categoryId IS NULL OR k.category.id = CAST(:categoryId AS string))
          AND (:status IS NULL OR k.status = :status)
          AND (:linkedEntityType IS NULL OR k.linkedEntityType = :linkedEntityType)
          AND (:linkedEntityId IS NULL OR k.linkedEntityId = CAST(:linkedEntityId AS string))
          AND (:ownerId IS NULL OR k.createdBy.id = CAST(:ownerId AS string))
    """)
    Page<Kri> findAllWithFilters(
            @Param("name")             String name,
            @Param("categoryId")       String categoryId,
            @Param("status")           KriStatus status,
            @Param("linkedEntityType") KriLinkedEntityType linkedEntityType,
            @Param("linkedEntityId")   String linkedEntityId,
            @Param("ownerId")          String ownerId,
            Pageable pageable
    );

    List<Kri> findByDeletedFalseAndStatus(KriStatus status);

    @Query("""
        SELECT k FROM Kri k
        WHERE k.deleted = false
          AND k.status IN (
              com.project.grcplatform.constant.KriStatus.WARNING,
              com.project.grcplatform.constant.KriStatus.BREACH
          )
    """)
    List<Kri> findAllActiveAlerts();

    @Query("""
        SELECT k.status, COUNT(k) FROM Kri k
        WHERE k.deleted = false
        GROUP BY k.status
    """)
    List<Object[]> countByStatus();
}