package com.project.grcplatform.mapper;

import com.project.grcplatform.dto.GovernancePolicyRequestDTO;
import com.project.grcplatform.dto.GovernancePolicyResponseDTO;
import com.project.grcplatform.dto.PolicyTypeDTO;
import com.project.grcplatform.dto.UserSummaryDTO;
import com.project.grcplatform.model.GovernancePolicy;
import com.project.grcplatform.model.PolicyType;

public class GovernancePolicyMapper {

    private GovernancePolicyMapper() {}

    public static GovernancePolicyResponseDTO toDTO(GovernancePolicy policy) {
        PolicyTypeDTO policyTypeDto = policy.getPolicyType() != null
                ? new PolicyTypeDTO(policy.getPolicyType().getId(), policy.getPolicyType().getName(), policy.getPolicyType().getDescription())
                : null;

        return GovernancePolicyResponseDTO.builder()
                .id(policy.getId())
                .title(policy.getTitle())
                .description(policy.getDescription())
                .policyType(policyTypeDto)
                .status(policy.getStatus())
                .version(policy.getVersion())
                .effectiveDate(policy.getEffectiveDate())
                .expiryDate(policy.getExpiryDate())
                .createdBy(UserSummaryDTO.of(policy.getCreatedBy()))
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }

    public static GovernancePolicy toEntity(GovernancePolicyRequestDTO request) {
        return GovernancePolicy.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .policyType(null)  // resolved in service
                .version(request.getVersion())
                .effectiveDate(request.getEffectiveDate())
                .expiryDate(request.getExpiryDate())
                .build();
    }

    public static void updateEntity(GovernancePolicy policy, GovernancePolicyRequestDTO request) {
        if (request.getTitle() != null)         policy.setTitle(request.getTitle());
        if (request.getDescription() != null)   policy.setDescription(request.getDescription());
        if (request.getVersion() != null)       policy.setVersion(request.getVersion());
        if (request.getEffectiveDate() != null) policy.setEffectiveDate(request.getEffectiveDate());
        if (request.getExpiryDate() != null)    policy.setExpiryDate(request.getExpiryDate());
        // policyType resolved in service
    }

    public static void updateEntityWithType(GovernancePolicy policy, PolicyType policyType) {
        policy.setPolicyType(policyType);
    }
}
