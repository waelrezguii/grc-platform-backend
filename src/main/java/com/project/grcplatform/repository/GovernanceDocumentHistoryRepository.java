package com.project.grcplatform.repository;

import com.project.grcplatform.model.GovernanceDocumentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GovernanceDocumentHistoryRepository extends JpaRepository<GovernanceDocumentHistory, String> {
    List<GovernanceDocumentHistory> findByDocument_IdOrderByCreatedAtDesc(String documentId);
}