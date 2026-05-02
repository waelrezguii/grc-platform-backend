package com.project.grcplatform.dto;

import com.project.grcplatform.constant.ControlEffectiveness;
import com.project.grcplatform.constant.ControlStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ReviewRequestDTO {

    @NotNull(message = "Effectiveness is required")
    private ControlEffectiveness effectiveness;

    @NotNull(message = "Status is required")
    private ControlStatus status;

    private LocalDate nextReviewDate;

    private String implementationNotes;

    private String evidenceUrl;
}