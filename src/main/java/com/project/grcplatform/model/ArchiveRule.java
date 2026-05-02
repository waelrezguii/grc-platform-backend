package com.project.grcplatform.model;

import com.project.grcplatform.base.BaseEntity;
import com.project.grcplatform.constant.ArchivableEntityType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "archive_rules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ArchiveRule extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    private ArchivableEntityType entityType;

    /**
     * Status value (stored as string) that triggers the archiving countdown.
     * Validated at the service layer against the entity's actual enum.
     */
    @Column(name = "trigger_status", nullable = false)
    private String triggerStatus;

    /**
     * Number of days a record must remain in the trigger status before being archived.
     */
    @Column(name = "delay_days", nullable = false)
    private int delayDays;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    /** Populated after each run — when the rule was last executed. */
    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    /** How many records were archived in the last run. */
    @Column(name = "last_run_archived")
    private Integer lastRunArchived;
}
