package com.project.grcplatform.repository;

import com.project.grcplatform.model.AuditChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AuditChecklistItemRepository extends JpaRepository<AuditChecklistItem, String> {

    Optional<AuditChecklistItem> findByIdAndDeletedFalse(String id);

    @Query("""
        SELECT i FROM AuditChecklistItem i
        WHERE i.deleted = false
          AND i.campaign.id = :campaignId
        ORDER BY i.createdAt ASC
    """)
    List<AuditChecklistItem> findByCampaignIdAndDeletedFalseOrderByCreatedAtAsc(
            @Param("campaignId") String campaignId);

    @Query("""
        SELECT i.result, COUNT(i) FROM AuditChecklistItem i
        WHERE i.campaign.id = :campaignId AND i.deleted = false
        GROUP BY i.result
    """)
    List<Object[]> countByResultForCampaign(@Param("campaignId") String campaignId);
}