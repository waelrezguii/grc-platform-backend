package com.project.grcplatform.model;

import com.project.grcplatform.base.SoftDeleteEntity;
import com.project.grcplatform.constant.WatchSourceType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "threat_watch_sources")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ThreatWatchSource extends SoftDeleteEntity {

    @Column(nullable = false)
    private String name;

    /** RSS/Atom or API endpoint URL. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WatchSourceType type;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    /** Timestamp of the last successful fetch. */
    @Column(name = "last_fetch_at")
    private LocalDateTime lastFetchAt;

    /** Number of new entries imported during the last fetch. */
    @Column(name = "last_fetch_count")
    @Builder.Default
    private Integer lastFetchCount = 0;
}
