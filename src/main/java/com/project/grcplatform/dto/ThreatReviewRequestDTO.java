package com.project.grcplatform.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ThreatReviewRequestDTO {

    /** Reviewer's notes on the current threat profile (required). */
    @NotNull
    private String notes;

    /**
     * Due date for the next scheduled review.
     * If omitted, defaults to 6 months from now.
     */
    private LocalDateTime nextReviewDue;
}
