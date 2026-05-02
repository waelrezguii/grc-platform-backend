package com.project.grcplatform.repository;

import com.project.grcplatform.model.AssetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssetTypeRepository extends JpaRepository<AssetType, String> {
    Optional<AssetType> findByName(String name);
    boolean existsByName(String name);
}
