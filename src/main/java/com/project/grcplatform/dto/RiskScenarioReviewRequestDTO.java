package com.project.grcplatform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RiskScenarioReviewRequestDTO {

    /** Reviewer's observations on the current scenario (required). */
    @NotBlank
    private String notes;

    /**
     * Due date for the next annual review.
     * Defaults to +1 year from now if omitted.
     */
    private LocalDateTime nextReviewDue;
}
