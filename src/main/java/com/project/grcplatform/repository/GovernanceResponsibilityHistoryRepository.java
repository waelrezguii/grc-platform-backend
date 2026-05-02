package com.project.grcplatform.repository;

import com.project.grcplatform.model.GovernanceResponsibilityHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GovernanceResponsibilityHistoryRepository extends JpaRepository<GovernanceResponsibilityHistory, String> {
    List<GovernanceResponsibilityHistory> findByResponsibility_IdOrderByCreatedAtDesc(String responsibilityId);
}