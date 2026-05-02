package com.project.grcplatform.repository;

import com.project.grcplatform.model.TreatmentPlanHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TreatmentPlanHistoryRepository extends JpaRepository<TreatmentPlanHistory, String> {
    List<TreatmentPlanHistory> findByPlan_IdOrderByCreatedAtDesc(String planId);
}