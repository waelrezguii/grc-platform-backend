package com.project.grcplatform.dto;

import com.project.grcplatform.constant.ActionPriority;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TreatmentActionRequestDTO {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private ActionPriority priority;

    private String assigneeId;

    private LocalDate dueDate;

    private String notes;
}