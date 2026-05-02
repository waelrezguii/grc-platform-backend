package com.project.grcplatform.service;

import com.project.grcplatform.constant.*;
import com.project.grcplatform.model.*;
import com.project.grcplatform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportBuilderService {

    private final RiskAssessmentRepository assessmentRepository;
    private final RiskAssessmentScenarioRepository assessmentScenarioRepository;
    private final RiskScenarioRepository scenarioRepository;
    private final AssetRepository assetRepository;
    private final ThreatRepository threatRepository;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final TreatmentPlanRepository treatmentPlanRepository;
    private final TreatmentActionRepository actionRepository;
    private final ControlRepository controlRepository;

    public Map<String, Object> build(ReportType type, String assessmentId, String organisationId) {
        return switch (type) {
            case ASSESSMENT_SUMMARY    -> buildAssessmentSummary(assessmentId);
            case RISK_MATRIX           -> buildRiskMatrix(assessmentId);
            case TREATMENT_STATUS      -> buildTreatmentStatus(assessmentId);
            case CONTROL_EFFECTIVENESS -> buildControlEffectiveness(organisationId);
            case VULNERABILITY_SUMMARY -> buildVulnerabilitySummary(organisationId);
            case EXECUTIVE_SUMMARY     -> buildExecutiveSummary(organisationId);
        };
    }

    // ---- ASSESSMENT SUMMARY ----
    private Map<String, Object> buildAssessmentSummary(String assessmentId) {
        Map<String, Object> payload = new LinkedHashMap<>();

        RiskAssessment assessment = assessmentRepository.findByIdAndDeletedFalse(assessmentId)
                .orElse(null);
        if (assessment == null) {
            payload.put("error", "Assessment not found");
            return payload;
        }

        payload.put("assessmentId", assessment.getId());
        payload.put("title", assessment.getTitle());
        payload.put("status", assessment.getStatus());
        payload.put("startDate", assessment.getStartDate());
        payload.put("endDate", assessment.getEndDate());
        payload.put("overallRiskScore", assessment.getOverallRiskScore());
        payload.put("overallResidualScore", assessment.getOverallResidualScore());
        payload.put("treatmentStrategy", assessment.getTreatmentStrategy());
        payload.put("approvedBy", assessment.getApprovedBy());
        payload.put("approvedAt", assessment.getApprovedAt());

        // Linked scenarios with details
        List<RiskAssessmentScenario> links = assessmentScenarioRepository.findByAssessment_Id(assessmentId);
        List<Map<String, Object>> scenarios = links.stream().map(link -> {
            Map<String, Object> s = new LinkedHashMap<>();
            scenarioRepository.findByIdAndDeletedFalse(link.getScenario().getId()).ifPresent(scenario -> {
                s.put("scenarioId", scenario.getId());
                s.put("name", scenario.getName());
                s.put("likelihood", scenario.getLikelihood());
                s.put("impact", scenario.getImpact());
                s.put("rawRiskScore", scenario.getRawRiskScore());
                s.put("residualRiskScore", scenario.getResidualRiskScore());
                s.put("status", scenario.getStatus());
                s.put("treatmentStrategy", link.getTreatmentStrategy());
                s.put("treatmentNotes", link.getTreatmentNotes());
                if (scenario.getAsset() != null)
                    assetRepository.findByIdAndDeletedFalse(scenario.getAsset().getId())
                            .ifPresent(a -> s.put("assetName", a.getName()));
                if (scenario.getThreat() != null)
                    threatRepository.findByIdAndDeletedFalse(scenario.getThreat().getId())
                            .ifPresent(t -> s.put("threatName", t.getName()));
            });
            return s;
        }).collect(Collectors.toList());

        payload.put("scenarios", scenarios);
        payload.put("scenarioCount", scenarios.size());
        return payload;
    }

    // ---- RISK MATRIX ----
    private Map<String, Object> buildRiskMatrix(String assessmentId) {
        Map<String, Object> payload = new LinkedHashMap<>();

        List<RiskScenario> scenarios = assessmentId != null
                ? assessmentScenarioRepository.findByAssessment_Id(assessmentId).stream()
                .map(link -> scenarioRepository.findByIdAndDeletedFalse(link.getScenario().getId()))
                .filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList())
                : scenarioRepository.findAll().stream()
                .filter(s -> !s.getDeleted()).collect(Collectors.toList());

        // Build 4x4 matrix cells
        Map<String, List<Map<String, Object>>> matrix = new LinkedHashMap<>();
        for (int l = 1; l <= 4; l++) {
            for (int i = 1; i <= 4; i++) {
                matrix.put(l + "x" + i, new ArrayList<>());
            }
        }

        List<Map<String, Object>> scenarioList = new ArrayList<>();
        for (RiskScenario s : scenarios) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", s.getId());
            entry.put("name", s.getName());
            entry.put("likelihood", s.getLikelihood());
            entry.put("impact", s.getImpact());
            entry.put("rawRiskScore", s.getRawRiskScore());
            entry.put("residualRiskScore", s.getResidualRiskScore());
            entry.put("riskLevel", getRiskLevel(s.getRawRiskScore()));
            scenarioList.add(entry);

            String key = s.getLikelihood() + "x" + s.getImpact();
            matrix.getOrDefault(key, new ArrayList<>()).add(entry);
        }

        payload.put("matrix", matrix);
        payload.put("scenarios", scenarioList);
        payload.put("totalScenarios", scenarioList.size());
        payload.put("criticalCount", scenarioList.stream().filter(e -> "CRITICAL".equals(e.get("riskLevel"))).count());
        payload.put("highCount", scenarioList.stream().filter(e -> "HIGH".equals(e.get("riskLevel"))).count());
        payload.put("mediumCount", scenarioList.stream().filter(e -> "MEDIUM".equals(e.get("riskLevel"))).count());
        payload.put("lowCount", scenarioList.stream().filter(e -> "LOW".equals(e.get("riskLevel"))).count());
        return payload;
    }

    // ---- TREATMENT STATUS ----
    private Map<String, Object> buildTreatmentStatus(String assessmentId) {
        Map<String, Object> payload = new LinkedHashMap<>();

        List<TreatmentPlan> plans = treatmentPlanRepository.findAll().stream()
                .filter(p -> !p.getDeleted())
                .filter(p -> assessmentId == null || (p.getAssessment() != null && assessmentId.equals(p.getAssessment().getId())))
                .collect(Collectors.toList());

        long draft     = plans.stream().filter(p -> p.getStatus() == TreatmentPlanStatus.DRAFT).count();
        long approved  = plans.stream().filter(p -> p.getStatus() == TreatmentPlanStatus.APPROVED).count();
        long inProgress= plans.stream().filter(p -> p.getStatus() == TreatmentPlanStatus.IN_PROGRESS).count();
        long completed = plans.stream().filter(p -> p.getStatus() == TreatmentPlanStatus.COMPLETED).count();
        long cancelled = plans.stream().filter(p -> p.getStatus() == TreatmentPlanStatus.CANCELLED).count();

        payload.put("totalPlans", plans.size());
        payload.put("byStatus", Map.of(
                "DRAFT", draft, "APPROVED", approved,
                "IN_PROGRESS", inProgress, "COMPLETED", completed, "CANCELLED", cancelled
        ));

        double avgProgress = plans.stream()
                .mapToInt(p -> p.getProgressPct() != null ? p.getProgressPct() : 0)
                .average().orElse(0);
        payload.put("averageProgressPct", Math.round(avgProgress));

        List<Map<String, Object>> planDetails = plans.stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("title", p.getTitle());
            m.put("status", p.getStatus());
            m.put("progressPct", p.getProgressPct());
            m.put("dueDate", p.getDueDate());
            m.put("treatmentStrategy", p.getTreatmentStrategy());
            m.put("estimatedBudget", p.getEstimatedBudget());
            m.put("actualCost", p.getActualCost());

            List<TreatmentAction> actions = actionRepository
                    .findByPlan_IdAndDeletedFalseOrderByCreatedAtAsc(p.getId());
            long doneTasks = actions.stream().filter(a -> a.getStatus() == ActionStatus.DONE).count();
            m.put("totalActions", actions.size());
            m.put("completedActions", doneTasks);
            return m;
        }).collect(Collectors.toList());

        payload.put("plans", planDetails);
        return payload;
    }

    // ---- CONTROL EFFECTIVENESS ----
    private Map<String, Object> buildControlEffectiveness(String organisationId) {
        Map<String, Object> payload = new LinkedHashMap<>();

        List<Control> controls = controlRepository.findAll().stream()
                .filter(c -> !c.getDeleted())
                .collect(Collectors.toList());

        long planned     = controls.stream().filter(c -> c.getStatus() == ControlStatus.PLANNED).count();
        long implemented = controls.stream().filter(c -> c.getStatus() == ControlStatus.IMPLEMENTED).count();
        long underReview = controls.stream().filter(c -> c.getStatus() == ControlStatus.UNDER_REVIEW).count();
        long ineffective = controls.stream().filter(c -> c.getStatus() == ControlStatus.INEFFECTIVE).count();
        long retired     = controls.stream().filter(c -> c.getStatus() == ControlStatus.RETIRED).count();

        payload.put("totalControls", controls.size());
        payload.put("byStatus", Map.of(
                "PLANNED", planned, "IMPLEMENTED", implemented,
                "UNDER_REVIEW", underReview, "INEFFECTIVE", ineffective, "RETIRED", retired
        ));

        Map<String, Long> byEffectiveness = new LinkedHashMap<>();
        byEffectiveness.put("FULLY_EFFECTIVE",    controls.stream().filter(c -> c.getEffectiveness() == ControlEffectiveness.FULLY_EFFECTIVE).count());
        byEffectiveness.put("LARGELY_EFFECTIVE",  controls.stream().filter(c -> c.getEffectiveness() == ControlEffectiveness.LARGELY_EFFECTIVE).count());
        byEffectiveness.put("PARTIAL",            controls.stream().filter(c -> c.getEffectiveness() == ControlEffectiveness.PARTIAL).count());
        byEffectiveness.put("INEFFECTIVE",        controls.stream().filter(c -> c.getEffectiveness() == ControlEffectiveness.INEFFECTIVE).count());
        byEffectiveness.put("NOT_REVIEWED",       controls.stream().filter(c -> c.getEffectiveness() == null).count());
        payload.put("byEffectiveness", byEffectiveness);

        Map<String, Long> byCategory = controls.stream()
                .filter(c -> c.getCategory() != null)
                .collect(Collectors.groupingBy(c -> c.getCategory().getName(), Collectors.counting()));
        payload.put("byCategory", byCategory);

        List<Map<String, Object>> controlDetails = controls.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("isoReference", c.getIsoReference());
            m.put("title", c.getTitle());
            m.put("category", c.getCategory());
            m.put("type", c.getType());
            m.put("status", c.getStatus());
            m.put("effectiveness", c.getEffectiveness());
            m.put("lastReviewDate", c.getLastReviewDate());
            m.put("nextReviewDate", c.getNextReviewDate());
            return m;
        }).collect(Collectors.toList());

        payload.put("controls", controlDetails);
        return payload;
    }

    // ---- VULNERABILITY SUMMARY ----
    private Map<String, Object> buildVulnerabilitySummary(String organisationId) {
        Map<String, Object> payload = new LinkedHashMap<>();

        List<Vulnerability> vulns = vulnerabilityRepository.findAll().stream()
                .filter(v -> !v.getDeleted())
                .collect(Collectors.toList());

        Map<String, Long> byCriticality = new LinkedHashMap<>();
        for (VulnCriticality c : VulnCriticality.values()) {
            byCriticality.put(c.name(), vulns.stream().filter(v -> v.getCriticality() == c).count());
        }

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (VulnStatus s : VulnStatus.values()) {
            byStatus.put(s.name(), vulns.stream().filter(v -> v.getStatus() == s).count());
        }

        payload.put("totalVulnerabilities", vulns.size());
        payload.put("byCriticality", byCriticality);
        payload.put("byStatus", byStatus);
        payload.put("openCritical", vulns.stream()
                .filter(v -> v.getCriticality() == VulnCriticality.CRITICAL && v.getStatus() == VulnStatus.OPEN)
                .count());

        List<Map<String, Object>> vulnDetails = vulns.stream()
                .sorted(Comparator.comparing(Vulnerability::getCriticality).reversed())
                .map(v -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", v.getId());
                    m.put("title", v.getTitle());
                    m.put("criticality", v.getCriticality());
                    m.put("status", v.getStatus());
                    m.put("source", v.getSource());
                    m.put("cvssScore", v.getCvssScore());
                    m.put("cveId", v.getCveId());
                    m.put("assetId", v.getAsset() != null ? v.getAsset().getId() : null);
                    m.put("detectedAt", v.getDetectedAt());
                    return m;
                }).collect(Collectors.toList());

        payload.put("vulnerabilities", vulnDetails);
        return payload;
    }

    // ---- EXECUTIVE SUMMARY ----
    private Map<String, Object> buildExecutiveSummary(String organisationId) {
        Map<String, Object> payload = new LinkedHashMap<>();

        // Assets
        long totalAssets = assetRepository.findAll().stream().filter(a -> !a.getDeleted()).count();

        // Vulnerabilities
        List<Vulnerability> vulns = vulnerabilityRepository.findAll().stream().filter(v -> !v.getDeleted()).collect(Collectors.toList());
        long openVulns    = vulns.stream().filter(v -> v.getStatus() == VulnStatus.OPEN).count();
        long criticalVulns = vulns.stream().filter(v -> v.getCriticality() == VulnCriticality.CRITICAL).count();

        // Scenarios
        List<RiskScenario> scenarios = scenarioRepository.findAll().stream().filter(s -> !s.getDeleted()).collect(Collectors.toList());
        OptionalDouble avgRisk = scenarios.stream().mapToInt(s -> s.getRawRiskScore() != null ? s.getRawRiskScore() : 0).average();
        OptionalDouble avgResidual = scenarios.stream().mapToInt(s -> s.getResidualRiskScore() != null ? s.getResidualRiskScore() : 0).average();

        // Treatment Plans
        List<TreatmentPlan> plans = treatmentPlanRepository.findAll().stream().filter(p -> !p.getDeleted()).collect(Collectors.toList());
        long completedPlans = plans.stream().filter(p -> p.getStatus() == TreatmentPlanStatus.COMPLETED).count();
        long inProgressPlans = plans.stream().filter(p -> p.getStatus() == TreatmentPlanStatus.IN_PROGRESS).count();

        // Controls
        List<Control> controls = controlRepository.findAll().stream().filter(c -> !c.getDeleted()).collect(Collectors.toList());
        long implementedControls = controls.stream().filter(c -> c.getStatus() == ControlStatus.IMPLEMENTED).count();
        long ineffectiveControls = controls.stream().filter(c -> c.getStatus() == ControlStatus.INEFFECTIVE).count();

        payload.put("kpis", Map.of(
                "totalAssets", totalAssets,
                "totalVulnerabilities", vulns.size(),
                "openVulnerabilities", openVulns,
                "criticalVulnerabilities", criticalVulns,
                "totalScenarios", scenarios.size(),
                "averageRawRiskScore", avgRisk.isPresent() ? Math.round(avgRisk.getAsDouble() * 10.0) / 10.0 : 0,
                "averageResidualRiskScore", avgResidual.isPresent() ? Math.round(avgResidual.getAsDouble() * 10.0) / 10.0 : 0,
                "totalTreatmentPlans", plans.size()
        ));

        payload.put("treatmentPlans", Map.of(
                "completed", completedPlans,
                "inProgress", inProgressPlans,
                "total", plans.size()
        ));

        payload.put("controls", Map.of(
                "total", controls.size(),
                "implemented", implementedControls,
                "ineffective", ineffectiveControls
        ));

        // Top 5 highest risk scenarios
        List<Map<String, Object>> topRisks = scenarios.stream()
                .filter(s -> s.getRawRiskScore() != null)
                .sorted(Comparator.comparingInt(RiskScenario::getRawRiskScore).reversed())
                .limit(5)
                .map(s -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", s.getId());
                    m.put("name", s.getName());
                    m.put("rawRiskScore", s.getRawRiskScore());
                    m.put("residualRiskScore", s.getResidualRiskScore());
                    m.put("riskLevel", getRiskLevel(s.getRawRiskScore()));
                    return m;
                }).collect(Collectors.toList());

        payload.put("topRisks", topRisks);
        return payload;
    }

    // ---- Helpers ----
    private String getRiskLevel(Short score) {
        if (score == null) return "UNKNOWN";
        if (score >= 12) return "CRITICAL";
        if (score >= 8)  return "HIGH";
        if (score >= 4)  return "MEDIUM";
        return "LOW";
    }
}