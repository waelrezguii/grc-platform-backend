package com.project.grcplatform.repository;

import com.project.grcplatform.model.ComplianceRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ComplianceRequirementRepository extends JpaRepository<ComplianceRequirement, String> {
    Optional<ComplianceRequirement> findByIdAndDeletedFalse(String id);
    List<ComplianceRequirement> findByFramework_IdAndDeletedFalseOrderByCodeAsc(String frameworkId);
}