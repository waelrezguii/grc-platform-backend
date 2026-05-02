package com.project.grcplatform.repository;

import com.project.grcplatform.model.ScenarioPriorityConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ScenarioPriorityConfigRepository extends JpaRepository<ScenarioPriorityConfig, String> {

    Optional<ScenarioPriorityConfig> findByIdAndDeletedFalse(String id);

    Optional<ScenarioPriorityConfig> findByActiveTrueAndDeletedFalse();

    @Query("""
            SELECT c FROM ScenarioPriorityConfig c
            WHERE c.deleted = false
            AND (CAST(:name AS String) IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', CAST(:name AS String), '%')))
            """)
    Page<ScenarioPriorityConfig> findAllWithFilters(@Param("name") String name, Pageable pageable);

    /** Deactivate every config except the one being activated. */
    @Modifying
    @Query("UPDATE ScenarioPriorityConfig c SET c.active = false WHERE c.id <> :excludeId AND c.deleted = false")
    void deactivateAllExcept(@Param("excludeId") String excludeId);
}
