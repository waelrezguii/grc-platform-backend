package com.project.grcplatform.dto;

import com.project.grcplatform.constant.TreatmentStrategy;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddScenarioRequestDTO {

    @NotBlank(message = "Scenario ID is required")
    private String scenarioId;

    private TreatmentStrategy treatmentStrategy;

    private String treatmentNotes;
}