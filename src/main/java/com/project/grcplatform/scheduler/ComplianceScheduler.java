package com.project.grcplatform.scheduler;

import com.project.grcplatform.constant.NotificationEntityType;
import com.project.grcplatform.constant.NotificationType;
import com.project.grcplatform.model.ComplianceActionPlan;
import com.project.grcplatform.repository.ComplianceActionPlanRepository;
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
public class ComplianceScheduler {

    private final ComplianceActionPlanRepository actionPlanRepository;
    private final NotificationService notificationService;

    /**
     * Every day at 08:20 — notify assignees of overdue compliance action plans.
     */
    @Scheduled(cron = "0 20 8 * * *")
    public void checkOverdueActionPlans() {
        LocalDate today = LocalDate.now();
        List<ComplianceActionPlan> overdue = actionPlanRepository.findOverduePlans(today);
        log.info("[ComplianceScheduler] Overdue action plans found: {}", overdue.size());

        for (ComplianceActionPlan plan : overdue) {
            if (plan.getAssignee() == null) continue;
            try {
                notificationService.notify(
                        plan.getAssignee().getId(),
                        NotificationType.COMPLIANCE_ACTION_OVERDUE,
                        NotificationEntityType.COMPLIANCE_ACTION_PLAN,
                        plan.getId(),
                        "⏰ Plan d'action de conformité en retard",
                        "Le plan d'action \"" + plan.getTitle() + "\" était dû le "
                                + plan.getDueDate() + " et n'est pas encore terminé."
                );
            } catch (Exception e) {
                log.error("[ComplianceScheduler] Failed to notify for plan {}: {}", plan.getId(), e.getMessage());
            }
        }
    }
}