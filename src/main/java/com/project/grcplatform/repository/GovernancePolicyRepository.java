package com.project.grcplatform.repository;

import com.project.grcplatform.constant.PolicyStatus;
import com.project.grcplatform.model.GovernancePolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GovernancePolicyRepository extends JpaRepository<GovernancePolicy, String> {

    Optional<GovernancePolicy> findByIdAndDeletedFalse(String id);

    @Query("""
        SELECT p FROM GovernancePolicy p
        WHERE p.deleted = false
          AND (:title IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', CAST(:title AS string), '%')))
          AND (:policyTypeId IS NULL OR p.policyType.id = CAST(:policyTypeId AS string))
          AND (:status IS NULL OR p.status = :status)
          AND (:ownerId IS NULL OR p.createdBy.id = CAST(:ownerId AS string))
    """)
    Page<GovernancePolicy> findAllWithFilters(
            @Param("title")        String title,
            @Param("policyTypeId") String policyTypeId,
            @Param("status")       PolicyStatus status,
            @Param("ownerId")      String ownerId,
            Pageable pageable
    );

    @Query("""
        SELECT p FROM GovernancePolicy p
        WHERE p.deleted = false
          AND p.status = com.project.grcplatform.constant.PolicyStatus.APPROVED
          AND p.expiryDate IS NOT NULL
          AND p.expiryDate <= :threshold
    """)
    List<GovernancePolicy> findExpiringSoon(@Param("threshold") LocalDate threshold);
}
