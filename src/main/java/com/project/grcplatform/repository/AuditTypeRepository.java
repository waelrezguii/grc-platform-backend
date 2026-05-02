package com.project.grcplatform.repository;

import com.project.grcplatform.model.AuditType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuditTypeRepository extends JpaRepository<AuditType, String> {
    Optional<AuditType> findByName(String name);
    boolean existsByName(String name);
}
