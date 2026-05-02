package com.project.grcplatform.model;

import com.project.grcplatform.base.BaseEntity;
import com.project.grcplatform.constant.SyncStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "scanner_sync_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScannerSyncLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scanner_id", nullable = false)
    private VulnerabilityScanner scanner;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SyncStatus status = SyncStatus.RUNNING;

    @Builder.Default
    @Column(name = "total_found")
    private int totalFound = 0;

    @Builder.Default
    private int imported = 0;

    @Builder.Default
    private int updated = 0;

    @Builder.Default
    private int skipped = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
