package com.project.grcplatform.repository;

import com.project.grcplatform.model.AssetHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetHistoryRepository extends JpaRepository<AssetHistory, String> {
    List<AssetHistory> findByAsset_IdOrderByCreatedAtDesc(String assetId);
}