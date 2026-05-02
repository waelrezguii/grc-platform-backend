package com.project.grcplatform.dto;

import com.project.grcplatform.constant.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ThreatRequestDTO {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;
    private String motivation;
    private String objectives;

    @NotNull(message = "Origin is required")
    private ThreatOrigin origin;

    private String typeId;

    @NotNull(message = "Frequency is required")
    private ThreatFrequency frequency;

    @NotNull(message = "Severity is required")
    private ThreatSeverity severity;

    private String scenario;
    private String vectors;
    private String aggravatingFactors;

    /** IDs of assets directly exposed to this threat. */
    private List<String> assetIds;

    /** IDs of vulnerabilities that this threat can exploit. */
    private List<String> vulnerabilityIds;
}
