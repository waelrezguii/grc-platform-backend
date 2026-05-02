package com.project.grcplatform.repository;

import com.project.grcplatform.constant.ArchivableEntityType;
import com.project.grcplatform.model.ArchiveRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArchiveRuleRepository extends JpaRepository<ArchiveRule, String> {

    List<ArchiveRule> findAllByEnabledTrue();

    List<ArchiveRule> findAllByEntityType(ArchivableEntityType entityType);
}
