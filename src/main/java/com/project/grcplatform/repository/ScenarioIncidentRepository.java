package com.project.grcplatform.repository;

import com.project.grcplatform.model.ScenarioIncident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScenarioIncidentRepository extends JpaRepository<ScenarioIncident, String> {
    List<ScenarioIncident> findByScenario_IdOrderByOccurredAtDesc(String scenarioId);
    Optional<ScenarioIncident> findByIdAndScenario_Id(String id, String scenarioId);
}
