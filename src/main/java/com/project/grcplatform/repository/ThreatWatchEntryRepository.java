package com.project.grcplatform.repository;

import com.project.grcplatform.model.ThreatWatchEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ThreatWatchEntryRepository extends JpaRepository<ThreatWatchEntry, String> {

    boolean existsBySource_IdAndEntryUrl(String sourceId, String entryUrl);

    @Query("""
        SELECT e FROM ThreatWatchEntry e
        WHERE (:sourceId IS NULL OR e.source.id = CAST(:sourceId AS string))
          AND (:processed IS NULL OR e.processed = :processed)
    """)
    Page<ThreatWatchEntry> findAllWithFilters(
            @Param("sourceId") String sourceId,
            @Param("processed") Boolean processed,
            Pageable pageable
    );

    Optional<ThreatWatchEntry> findById(String id);
}
