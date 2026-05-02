package com.project.grcplatform.repository;

import com.project.grcplatform.constant.FrameworkStatus;
import com.project.grcplatform.constant.FrameworkType;
import com.project.grcplatform.model.ComplianceFramework;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ComplianceFrameworkRepository extends JpaRepository<ComplianceFramework, String> {

    Optional<ComplianceFramework> findByIdAndDeletedFalse(String id);

    @Query("""
        SELECT f FROM ComplianceFramework f
        WHERE f.deleted = false
          AND (:name IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%')))
          AND (:frameworkType IS NULL OR f.frameworkType = :frameworkType)
          AND (:status IS NULL OR f.status = :status)
    """)
    Page<ComplianceFramework> findAllWithFilters(
            @Param("name")          String name,
            @Param("frameworkType") FrameworkType frameworkType,
            @Param("status")        FrameworkStatus status,
            Pageable pageable
    );
}