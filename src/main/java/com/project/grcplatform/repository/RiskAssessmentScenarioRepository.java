package com.project.grcplatform.repository;

import com.project.grcplatform.model.RiskAssessmentScenario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RiskAssessmentScenarioRepository extends JpaRepository<RiskAssessmentScenario, String> {

    List<RiskAssessmentScenario> findByAssessment_Id(String assessmentId);

    Optional<RiskAssessmentScenario> findByAssessment_IdAndScenario_Id(String assessmentId, String scenarioId);

    boolean existsByAssessment_IdAndScenario_Id(String assessmentId, String scenarioId);

    void deleteByAssessment_IdAndScenario_Id(String assessmentId, String scenarioId);
}