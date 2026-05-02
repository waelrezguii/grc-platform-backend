package com.project.grcplatform.repository;

import com.project.grcplatform.model.GovernancePolicyHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GovernancePolicyHistoryRepository extends JpaRepository<GovernancePolicyHistory, String> {
    List<GovernancePolicyHistory> findByPolicy_IdOrderByCreatedAtDesc(String policyId);
}