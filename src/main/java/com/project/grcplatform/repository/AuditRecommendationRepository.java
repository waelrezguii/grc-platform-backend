package com.project.grcplatform.repository;

import com.project.grcplatform.constant.RecommendationPriority;
import com.project.grcplatform.constant.RecommendationStatus;
import com.project.grcplatform.model.AuditRecommendation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AuditRecommendationRepository extends JpaRepository<AuditRecommendation, String> {

    Optional<AuditRecommendation> findByIdAndDeletedFalse(String id);

    List<AuditRecommendation> findByCampaignIdAndDeletedFalseOrderByCreatedAtDesc(String campaignId);

    @Query("""
        SELECT r FROM AuditRecommendation r
        LEFT JOIN r.assignee a
        WHERE r.deleted = false
          AND (:campaignId IS NULL OR r.campaign.id = CAST(:campaignId AS string))
          AND (:findingId  IS NULL OR r.finding.id  = CAST(:findingId  AS string))
          AND (:status     IS NULL OR r.status    = :status)
          AND (:assigneeId IS NULL OR a.id        = CAST(:assigneeId AS string))
          AND (:priority   IS NULL OR r.priority  = :priority)
    """)
    Page<AuditRecommendation> findAllWithFilters(
            @Param("campaignId")  String campaignId,
            @Param("findingId")   String findingId,
            @Param("status")      RecommendationStatus status,
            @Param("assigneeId")  String assigneeId,
            @Param("priority")    RecommendationPriority priority,
            Pageable pageable
    );

    @Query("""
        SELECT r FROM AuditRecommendation r
        WHERE r.deleted = false
          AND r.dueDate < :today
          AND r.status NOT IN (
              com.project.grcplatform.constant.RecommendationStatus.IMPLEMENTED,
              com.project.grcplatform.constant.RecommendationStatus.REJECTED,
              com.project.grcplatform.constant.RecommendationStatus.CLOSED
          )
    """)
    List<AuditRecommendation> findOverdueRecommendations(@Param("today") LocalDate today);
}