package com.project.grcplatform.repository;

import com.project.grcplatform.constant.ResponsibilityStatus;
import com.project.grcplatform.model.GovernanceResponsibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GovernanceResponsibilityRepository extends JpaRepository<GovernanceResponsibility, String> {

    Optional<GovernanceResponsibility> findByIdAndDeletedFalse(String id);

    @Query("""
        SELECT r FROM GovernanceResponsibility r
        WHERE r.deleted = false
          AND (:title IS NULL OR LOWER(r.title) LIKE LOWER(CONCAT('%', CAST(:title AS string), '%')))
          AND (:responsibilityTypeId IS NULL OR r.responsibilityType.id = CAST(:responsibilityTypeId AS string))
          AND (:status IS NULL OR r.status = :status)
          AND (:assigneeId IS NULL OR r.assignee.id = CAST(:assigneeId AS string))
    """)
    Page<GovernanceResponsibility> findAllWithFilters(
            @Param("title")                  String title,
            @Param("responsibilityTypeId")   String responsibilityTypeId,
            @Param("status")                 ResponsibilityStatus status,
            @Param("assigneeId")             String assigneeId,
            Pageable pageable
    );
}
