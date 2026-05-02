package com.project.grcplatform.repository;

import com.project.grcplatform.model.RiskAssessmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiskAssessmentHistoryRepository extends JpaRepository<RiskAssessmentHistory, String> {
    List<RiskAssessmentHistory> findByAssessment_IdOrderByCreatedAtDesc(String assessmentId);
}