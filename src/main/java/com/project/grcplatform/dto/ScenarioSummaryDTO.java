package com.project.grcplatform.dto;

import com.project.grcplatform.model.RiskScenario;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioSummaryDTO {

    private String id;
    private String name;

    public static ScenarioSummaryDTO of(RiskScenario s) {
        if (s == null) return null;
        return new ScenarioSummaryDTO(s.getId(), s.getName());
    }
}
