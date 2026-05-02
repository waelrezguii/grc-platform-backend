package com.project.grcplatform.repository;

import com.project.grcplatform.model.RiskScenarioHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiskScenarioHistoryRepository extends JpaRepository<RiskScenarioHistory, String> {
    List<RiskScenarioHistory> findByScenario_IdOrderByVersionNumberAsc(String scenarioId);
    long countByScenario_Id(String scenarioId);
}