package com.project.grcplatform.scheduler;

import com.project.grcplatform.constant.NotificationEntityType;
import com.project.grcplatform.constant.NotificationType;
import com.project.grcplatform.model.AuditRecommendation;
import com.project.grcplatform.repository.AuditRecommendationRepository;
import com.project.grcplatform.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditScheduler {

    private final AuditRecommendationRepository recommendationRepository;
    private final NotificationService notificationService;

    /**
     * Every day at 08:25 — notify assignees of overdue audit recommendations.
     */
    @Scheduled(cron = "0 25 8 * * *")
    public void checkOverdueRecommendations() {
        LocalDate today = LocalDate.now();
        List<AuditRecommendation> overdue = recommendationRepository.findOverdueRecommendations(today);
        log.info("[AuditScheduler] Found {} overdue audit recommendations", overdue.size());

        for (AuditRecommendation r : overdue) {
            String targetId = r.getAssignee() != null ? r.getAssignee().getId()
                    : r.getOwner()   != null ? r.getOwner().getId()
                    : null;
            if (targetId == null) continue;
            try {
                notificationService.notify(
                        targetId,
                        NotificationType.AUDIT_RECOMMENDATION_OVERDUE,
                        NotificationEntityType.AUDIT_RECOMMENDATION,
                        r.getId(),
                        "Recommandation en retard",
                        "La recommandation \"" + r.getTitle() + "\" était due le "
                                + r.getDueDate() + " et n'est pas encore implémentée."
                );
            } catch (Exception e) {
                log.error("[AuditScheduler] Failed to notify for recommendation {}: {}", r.getId(), e.getMessage());
            }
        }
    }
}