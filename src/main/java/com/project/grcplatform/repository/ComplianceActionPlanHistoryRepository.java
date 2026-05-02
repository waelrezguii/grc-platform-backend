package com.project.grcplatform.repository;

import com.project.grcplatform.model.ComplianceActionPlanHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ComplianceActionPlanHistoryRepository extends JpaRepository<ComplianceActionPlanHistory, String> {
    List<ComplianceActionPlanHistory> findByActionPlan_IdOrderByCreatedAtDesc(String actionPlanId);
}