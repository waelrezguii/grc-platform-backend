package com.project.grcplatform.repository;

import com.project.grcplatform.model.KriHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KriHistoryRepository extends JpaRepository<KriHistory, String> {

    List<KriHistory> findByKri_IdOrderByCreatedAtDesc(String kriId);
}