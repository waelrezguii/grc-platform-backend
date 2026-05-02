package com.project.grcplatform.repository;

import com.project.grcplatform.constant.LifecycleStatus;
import com.project.grcplatform.model.Asset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, String> {

    Optional<Asset> findByIdAndDeletedFalse(String id);

    long countByType_NameAndDeletedFalse(String typeName);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(a.ref, 2) AS int)), 0) FROM Asset a WHERE a.type.name = :typeName")
    int findMaxRefNumberByTypeName(@Param("typeName") String typeName);

    boolean existsByType_NameAndDeletedFalse(String typeName);

    List<Asset> findAllByDeletedFalse();

    List<Asset> findAllByEndOfLifeDateBetweenAndDeletedFalse(LocalDate from, LocalDate to);

    List<Asset> findAllByEndOfLifeDateBeforeAndDeletedFalse(LocalDate date);

    Optional<Asset> findFirstByIpAddressAndDeletedFalse(String ipAddress);

    Optional<Asset> findFirstByHostnameIgnoreCaseAndDeletedFalse(String hostname);

    @Query("""
        SELECT a FROM Asset a
        WHERE a.deleted = false
          AND (:name IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%')))
          AND (:categoryId IS NULL OR a.category.id = CAST(:categoryId AS string))
          AND (:typeId IS NULL OR a.type.id = CAST(:typeId AS string))
          AND (:lifecycleStatus IS NULL OR a.lifecycleStatus = :lifecycleStatus)
          AND (:ownerId IS NULL OR a.owner.id = CAST(:ownerId AS string))
          AND (:directionCentraleId IS NULL OR a.directionCentrale.id = CAST(:directionCentraleId AS string))
          AND (:directionId IS NULL OR a.direction.id = CAST(:directionId AS string))
    """)
    Page<Asset> findAllWithFilters(
            @Param("name") String name,
            @Param("categoryId") String categoryId,
            @Param("typeId") String typeId,
            @Param("lifecycleStatus") LifecycleStatus lifecycleStatus,
            @Param("ownerId") String ownerId,
            @Param("directionCentraleId") String directionCentraleId,
            @Param("directionId") String directionId,
            Pageable pageable
    );
}
