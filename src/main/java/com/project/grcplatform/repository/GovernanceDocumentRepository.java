package com.project.grcplatform.repository;

import com.project.grcplatform.constant.DocumentStatus;
import com.project.grcplatform.model.GovernanceDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GovernanceDocumentRepository extends JpaRepository<GovernanceDocument, String> {

    Optional<GovernanceDocument> findByIdAndDeletedFalse(String id);

    @Query("""
        SELECT d FROM GovernanceDocument d
        WHERE d.deleted = false
          AND (:title IS NULL OR LOWER(d.title) LIKE LOWER(CONCAT('%', CAST(:title AS string), '%')))
          AND (:documentTypeId IS NULL OR d.documentType.id = CAST(:documentTypeId AS string))
          AND (:status IS NULL OR d.status = :status)
          AND (:linkedPolicyId IS NULL OR d.linkedPolicy.id = CAST(:linkedPolicyId AS string))
    """)
    Page<GovernanceDocument> findAllWithFilters(
            @Param("title")          String title,
            @Param("documentTypeId") String documentTypeId,
            @Param("status")         DocumentStatus status,
            @Param("linkedPolicyId") String linkedPolicyId,
            Pageable pageable
    );
}
