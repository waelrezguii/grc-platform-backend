package com.project.grcplatform.mapper;

import com.project.grcplatform.dto.ImpactEvaluationResponseDTO;
import com.project.grcplatform.dto.UserSummaryDTO;
import com.project.grcplatform.model.Asset;
import com.project.grcplatform.model.ImpactEvaluation;
import com.project.grcplatform.model.RiskScenario;
import com.project.grcplatform.model.Vulnerability;

public class ImpactEvaluationMapper {

    public static ImpactEvaluationResponseDTO toDTO(ImpactEvaluation e) {
        if (e == null) return null;

        ImpactEvaluationResponseDTO dto = new ImpactEvaluationResponseDTO();
        dto.setId(e.getId());

        Vulnerability vuln = e.getVulnerability();
        if (vuln != null) {
            dto.setVulnerabilityId(vuln.getId());
            dto.setVulnerabilityTitle(vuln.getTitle());
            Asset asset = vuln.getAsset();
            if (asset != null) {
                dto.setAssetId(asset.getId());
                dto.setAssetName(asset.getName());
            }
        }

        RiskScenario scenario = e.getScenario();
        if (scenario != null) {
            dto.setScenarioId(scenario.getId());
            dto.setScenarioName(scenario.getName());
        }

        dto.setExploitationContext(e.getExploitationContext());
        dto.setCvssScoreUsed(e.getCvssScoreUsed());
        dto.setAssetCriticalityUsed(e.getAssetCriticalityUsed());
        dto.setComputedScore(e.getComputedScore());
        dto.setOverrideScore(e.getOverrideScore());
        dto.setOverrideJustification(e.getOverrideJustification());
        dto.setOverriddenBy(e.getOverriddenBy());
        dto.setOverriddenAt(e.getOverriddenAt());

        // Effective score: override takes precedence over computed
        dto.setEffectiveScore(e.getOverrideScore() != null ? e.getOverrideScore() : e.getComputedScore());

        dto.setCreatedBy(UserSummaryDTO.of(e.getCreatedBy()));
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());

        return dto;
    }
}
