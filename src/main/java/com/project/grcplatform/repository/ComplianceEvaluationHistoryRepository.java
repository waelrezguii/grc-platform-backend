package com.project.grcplatform.repository;

import com.project.grcplatform.model.ComplianceEvaluationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ComplianceEvaluationHistoryRepository extends JpaRepository<ComplianceEvaluationHistory, String> {
    List<ComplianceEvaluationHistory> findByEvaluation_IdOrderByCreatedAtDesc(String evaluationId);
}