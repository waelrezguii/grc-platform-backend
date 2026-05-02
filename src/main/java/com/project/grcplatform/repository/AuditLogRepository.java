package com.project.grcplatform.repository;

import com.project.grcplatform.constant.AuditAction;
import com.project.grcplatform.constant.AuditEntityType;
import com.project.grcplatform.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:userId IS NULL OR a.userId = CAST(:userId AS string))
          AND (:entityType IS NULL OR a.entityType = :entityType)
          AND (:entityId IS NULL OR a.entityId = CAST(:entityId AS string))
          AND (:action IS NULL OR a.action = :action)
          AND (:success IS NULL OR a.success = :success)
          AND (:from IS NULL OR a.createdAt >= :from)
          AND (:to IS NULL OR a.createdAt <= :to)
    """)
    Page<AuditLog> findAllWithFilters(
            @Param("userId") String userId,
            @Param("entityType") AuditEntityType entityType,
            @Param("entityId") String entityId,
            @Param("action") AuditAction action,
            @Param("success") Boolean success,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            AuditEntityType entityType, String entityId);

    List<AuditLog> findByUserIdOrderByCreatedAtDesc(String userId);

    /** Returns the most recently inserted entry — used to get the chain tip hash. */
    java.util.Optional<AuditLog> findTopByOrderByCreatedAtDesc();

    /** Returns all entries ordered oldest-first — used for chain verification. */
    List<AuditLog> findAllByOrderByCreatedAtAsc();
}