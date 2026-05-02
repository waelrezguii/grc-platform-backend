package com.project.grcplatform.repository;

import com.project.grcplatform.model.ScannerSyncLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScannerSyncLogRepository extends JpaRepository<ScannerSyncLog, String> {

    List<ScannerSyncLog> findAllByScanner_IdOrderByStartedAtDesc(String scannerId);

    List<ScannerSyncLog> findAllByOrderByStartedAtDesc();
}
