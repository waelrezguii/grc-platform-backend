package com.project.grcplatform.repository;

import com.project.grcplatform.model.ImpactEvaluationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImpactEvaluationHistoryRepository extends JpaRepository<ImpactEvaluationHistory, String> {

    List<ImpactEvaluationHistory> findByEvaluation_IdOrderByChangedAtDesc(String evaluationId);
}
