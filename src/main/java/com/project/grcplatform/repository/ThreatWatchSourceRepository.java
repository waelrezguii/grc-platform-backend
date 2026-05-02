package com.project.grcplatform.repository;

import com.project.grcplatform.model.ThreatWatchSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ThreatWatchSourceRepository extends JpaRepository<ThreatWatchSource, String> {

    Optional<ThreatWatchSource> findByIdAndDeletedFalse(String id);

    List<ThreatWatchSource> findByEnabledTrueAndDeletedFalse();

    @Query("""
        SELECT s FROM ThreatWatchSource s
        WHERE s.deleted = false
          AND (:enabled IS NULL OR s.enabled = :enabled)
    """)
    Page<ThreatWatchSource> findAllWithFilters(
            @Param("enabled") Boolean enabled,
            Pageable pageable
    );
}
