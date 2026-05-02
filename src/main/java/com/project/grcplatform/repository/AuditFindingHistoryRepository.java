package com.project.grcplatform.repository;

import com.project.grcplatform.model.AuditFindingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditFindingHistoryRepository extends JpaRepository<AuditFindingHistory, String> {
    List<AuditFindingHistory> findByFindingIdOrderByCreatedAtDesc(String findingId);
}