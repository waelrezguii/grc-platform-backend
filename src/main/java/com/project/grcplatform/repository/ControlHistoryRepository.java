package com.project.grcplatform.repository;

import com.project.grcplatform.model.ControlHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ControlHistoryRepository extends JpaRepository<ControlHistory, String> {
    List<ControlHistory> findByControl_IdOrderByCreatedAtDesc(String controlId);
}