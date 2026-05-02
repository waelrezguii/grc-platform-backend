package com.project.grcplatform.repository;

import com.project.grcplatform.constant.AuditCampaignStatus;
import com.project.grcplatform.model.AuditCampaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuditCampaignRepository extends JpaRepository<AuditCampaign, String> {

    Optional<AuditCampaign> findByIdAndDeletedFalse(String id);

    @Query("""
        SELECT c FROM AuditCampaign c
        LEFT JOIN c.auditor aud
        WHERE c.deleted = false
          AND c.archived = :archived
          AND (:title     IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', CAST(:title AS string), '%')))
          AND (:auditTypeId IS NULL OR c.auditType.id = CAST(:auditTypeId AS string))
          AND (:status    IS NULL OR c.status    = :status)
          AND (:auditorId IS NULL OR aud.id      = CAST(:auditorId AS string))
    """)
    Page<AuditCampaign> findAllWithFilters(
            @Param("title")       String title,
            @Param("auditTypeId") String auditTypeId,
            @Param("status")      AuditCampaignStatus status,
            @Param("auditorId")   String auditorId,
            @Param("archived")    boolean archived,
            Pageable pageable
    );

    @Query("""
        SELECT c FROM AuditCampaign c
        WHERE c.status = :status
          AND c.updatedAt < :before
          AND c.deleted = false
          AND c.archived = false
    """)
    List<AuditCampaign> findArchiveCandidates(
            @Param("status") AuditCampaignStatus status,
            @Param("before") LocalDateTime before
    );
}