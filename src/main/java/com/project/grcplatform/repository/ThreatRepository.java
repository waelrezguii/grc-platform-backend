package com.project.grcplatform.repository;

import com.project.grcplatform.constant.ThreatOrigin;
import com.project.grcplatform.constant.ThreatSeverity;
import com.project.grcplatform.constant.ThreatStatus;
import com.project.grcplatform.model.Threat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ThreatRepository extends JpaRepository<Threat, String> {

    Optional<Threat> findByIdAndDeletedFalse(String id);

    @Query("""
        SELECT t FROM Threat t
        WHERE t.deleted = false
          AND (:name IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%')))
          AND (:origin IS NULL OR t.origin = :origin)
          AND (:typeId IS NULL OR t.type.id = CAST(:typeId AS string))
          AND (:severity IS NULL OR t.severity = :severity)
          AND (:status IS NULL OR t.status = :status)
    """)
    Page<Threat> findAllWithFilters(
            @Param("name") String name,
            @Param("origin") ThreatOrigin origin,
            @Param("typeId") String typeId,
            @Param("severity") ThreatSeverity severity,
            @Param("status") ThreatStatus status,
            Pageable pageable
    );
}
