package com.project.grcplatform.repository;

import com.project.grcplatform.model.AuditCampaignHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditCampaignHistoryRepository extends JpaRepository<AuditCampaignHistory, String> {
    List<AuditCampaignHistory> findByCampaignIdOrderByCreatedAtDesc(String campaignId);
}