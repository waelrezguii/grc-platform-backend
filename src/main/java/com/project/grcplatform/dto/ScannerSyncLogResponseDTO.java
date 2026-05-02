package com.project.grcplatform.dto;

import com.project.grcplatform.constant.SyncStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScannerSyncLogResponseDTO {

    private String id;
    private String scannerId;
    private String scannerName;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private SyncStatus status;
    private int totalFound;
    private int imported;
    private int updated;
    private int skipped;
    private String errorMessage;
}
