package com.project.grcplatform.repository;

import com.project.grcplatform.constant.FindingSeverity;
import com.project.grcplatform.constant.FindingStatus;
import com.project.grcplatform.constant.FindingType;
import com.project.grcplatform.model.AuditFinding;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AuditFindingRepository extends JpaRepository<AuditFinding, String> {

    Optional<AuditFinding> findByIdAndDeletedFalse(String id);

    @Query("""
        SELECT f FROM AuditFinding f
        WHERE f.deleted = false
          AND f.campaign.id = :campaignId
        ORDER BY f.createdAt DESC
    """)
    List<AuditFinding> findByCampaignIdAndDeletedFalseOrderByCreatedAtDesc(@Param("campaignId") String campaignId);

    @Query("""
        SELECT f FROM AuditFinding f
        WHERE f.deleted = false
          AND (:campaignId  IS NULL OR f.campaign.id  = CAST(:campaignId  AS string))
          AND (:findingType IS NULL OR f.findingType  = :findingType)
          AND (:severity    IS NULL OR f.severity     = :severity)
          AND (:status      IS NULL OR f.status       = :status)
    """)
    Page<AuditFinding> findAllWithFilters(
            @Param("campaignId")  String campaignId,
            @Param("findingType") FindingType findingType,
            @Param("severity")    FindingSeverity severity,
            @Param("status")      FindingStatus status,
            Pageable pageable
    );
}