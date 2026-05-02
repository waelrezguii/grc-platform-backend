package com.project.grcplatform.repository;

import com.project.grcplatform.model.ScenarioPriorityConfigHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScenarioPriorityConfigHistoryRepository extends JpaRepository<ScenarioPriorityConfigHistory, String> {

    List<ScenarioPriorityConfigHistory> findByConfig_IdOrderByChangedAtDesc(String configId);
}
