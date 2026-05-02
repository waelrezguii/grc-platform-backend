package com.project.grcplatform.model;

import com.project.grcplatform.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "threat_watch_entries",
    uniqueConstraints = @UniqueConstraint(columnNames = {"source_id", "entry_url"})
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ThreatWatchEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private ThreatWatchSource source;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    /** Original URL of the advisory or article. Used for deduplication per source. */
    @Column(name = "entry_url", columnDefinition = "TEXT")
    private String entryUrl;

    /** Publication date as reported by the feed. */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    /**
     * True once an analyst has reviewed this entry
     * (linked it to a threat or dismissed it).
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean processed = false;

    /** Optional: threat this entry was linked to by an analyst. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_threat_id")
    private Threat linkedThreat;

    /** User who marked the entry as processed. */
    @Column(name = "processed_by")
    private String processedBy;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
