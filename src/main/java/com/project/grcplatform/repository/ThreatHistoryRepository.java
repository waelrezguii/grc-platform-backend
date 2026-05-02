package com.project.grcplatform.repository;

import com.project.grcplatform.model.ThreatHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ThreatHistoryRepository extends JpaRepository<ThreatHistory, String> {
    List<ThreatHistory> findByThreat_IdOrderByCreatedAtDesc(String threatId);
}