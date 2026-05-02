package com.project.grcplatform.repository;

import com.project.grcplatform.model.AuditRecommendationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditRecommendationHistoryRepository extends JpaRepository<AuditRecommendationHistory, String> {
    List<AuditRecommendationHistory> findByRecommendationIdOrderByCreatedAtDesc(String recommendationId);
}