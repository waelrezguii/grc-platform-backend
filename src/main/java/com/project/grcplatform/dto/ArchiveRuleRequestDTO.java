package com.project.grcplatform.dto;

import com.project.grcplatform.constant.ArchivableEntityType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ArchiveRuleRequestDTO {

    @NotNull(message = "Entity type is required")
    private ArchivableEntityType entityType;

    /**
     * The status value (as a string) that starts the archiving countdown.
     * Must match a valid enum value for the selected entity type.
     * Examples: RESOLVED, COMPLETED, VALIDATED, CANCELLED
     */
    @NotBlank(message = "Trigger status is required")
    private String triggerStatus;

    /**
     * Number of days the record must stay in the trigger status before being archived.
     * Minimum 1 day.
     */
    @NotNull(message = "Delay in days is required")
    @Min(value = 1, message = "Delay must be at least 1 day")
    private Integer delayDays;

    private String description;

    private Boolean enabled = true;
}
