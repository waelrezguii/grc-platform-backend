package com.project.grcplatform.mapper;

import com.project.grcplatform.dto.*;
import com.project.grcplatform.model.Threat;

import java.util.List;

public class ThreatMapper {

    public static ThreatResponseDTO toDTO(Threat threat) {
        if (threat == null) return null;

        ThreatTypeDTO typeDto = threat.getType() != null
                ? new ThreatTypeDTO(threat.getType().getId(), threat.getType().getName(), threat.getType().getDescription())
                : null;

        ThreatResponseDTO dto = new ThreatResponseDTO();
        dto.setId(threat.getId());
        dto.setName(threat.getName());
        dto.setDescription(threat.getDescription());
        dto.setMotivation(threat.getMotivation());
        dto.setObjectives(threat.getObjectives());
        dto.setOrigin(threat.getOrigin());
        dto.setType(typeDto);
        dto.setFrequency(threat.getFrequency());
        dto.setSeverity(threat.getSeverity());
        dto.setScenario(threat.getScenario());
        dto.setVectors(threat.getVectors());
        dto.setAggravatingFactors(threat.getAggravatingFactors());
        dto.setStatus(threat.getStatus());
        dto.setValidatedBy(threat.getValidatedBy());
        dto.setValidatedAt(threat.getValidatedAt());
        dto.setLastReviewedBy(threat.getLastReviewedBy());
        dto.setLastReviewedAt(threat.getLastReviewedAt());
        dto.setNextReviewDue(threat.getNextReviewDue());
        dto.setReviewNotes(threat.getReviewNotes());
        dto.setAssets(threat.getAssets() != null
                ? threat.getAssets().stream().map(AssetSummaryDTO::of).toList()
                : List.of());
        dto.setVulnerabilities(threat.getVulnerabilities() != null
                ? threat.getVulnerabilities().stream().map(VulnerabilitySummaryDTO::of).toList()
                : List.of());
        dto.setCreatedBy(UserSummaryDTO.of(threat.getOwner()));
        dto.setCreatedAt(threat.getCreatedAt());
        dto.setUpdatedAt(threat.getUpdatedAt());
        return dto;
    }

    public static Threat toEntity(ThreatRequestDTO request) {
        if (request == null) return null;

        return Threat.builder()
                .name(request.getName())
                .description(request.getDescription())
                .motivation(request.getMotivation())
                .objectives(request.getObjectives())
                .origin(request.getOrigin())
                .type(null)  // resolved in service
                .frequency(request.getFrequency())
                .severity(request.getSeverity())
                .scenario(request.getScenario())
                .vectors(request.getVectors())
                .aggravatingFactors(request.getAggravatingFactors())
                .build();
    }

    public static void updateEntity(Threat threat, ThreatRequestDTO request) {
        if (request.getName() != null)                threat.setName(request.getName());
        if (request.getDescription() != null)         threat.setDescription(request.getDescription());
        if (request.getMotivation() != null)          threat.setMotivation(request.getMotivation());
        if (request.getObjectives() != null)          threat.setObjectives(request.getObjectives());
        if (request.getOrigin() != null)              threat.setOrigin(request.getOrigin());
        if (request.getFrequency() != null)           threat.setFrequency(request.getFrequency());
        if (request.getSeverity() != null)            threat.setSeverity(request.getSeverity());
        if (request.getScenario() != null)            threat.setScenario(request.getScenario());
        if (request.getVectors() != null)             threat.setVectors(request.getVectors());
        if (request.getAggravatingFactors() != null)  threat.setAggravatingFactors(request.getAggravatingFactors());
        // type, assets, vulnerabilities resolved in service
    }
}
