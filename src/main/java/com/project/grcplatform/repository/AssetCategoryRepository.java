package com.project.grcplatform.repository;

import com.project.grcplatform.model.AssetCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssetCategoryRepository extends JpaRepository<AssetCategory, String> {
    Optional<AssetCategory> findByName(String name);
    boolean existsByName(String name);
}
