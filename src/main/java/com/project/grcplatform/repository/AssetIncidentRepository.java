package com.project.grcplatform.repository;

import com.project.grcplatform.model.AssetIncident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssetIncidentRepository extends JpaRepository<AssetIncident, String> {

    List<AssetIncident> findAllByAsset_IdOrderByOccurredAtDesc(String assetId);

    Optional<AssetIncident> findByIdAndAsset_Id(String id, String assetId);
}
