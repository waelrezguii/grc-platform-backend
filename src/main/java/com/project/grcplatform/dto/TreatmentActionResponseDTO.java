package com.project.grcplatform.dto;

import com.project.grcplatform.constant.ActionPriority;
import com.project.grcplatform.constant.ActionStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TreatmentActionResponseDTO {

    private String id;
    private String planId;
    private String title;
    private String description;
    private ActionPriority priority;
    private ActionStatus status;
    private String assigneeId;
    private LocalDate dueDate;
    private LocalDateTime completedAt;
    private String notes;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}