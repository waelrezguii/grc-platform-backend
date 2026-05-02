package com.project.grcplatform.repository;

import com.project.grcplatform.model.ComplianceFrameworkHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ComplianceFrameworkHistoryRepository extends JpaRepository<ComplianceFrameworkHistory, String> {
    List<ComplianceFrameworkHistory> findByFramework_IdOrderByCreatedAtDesc(String frameworkId);
}