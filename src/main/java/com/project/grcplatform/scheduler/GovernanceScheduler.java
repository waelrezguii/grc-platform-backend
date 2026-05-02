package com.project.grcplatform.scheduler;

import com.project.grcplatform.constant.NotificationEntityType;
import com.project.grcplatform.constant.NotificationType;
import com.project.grcplatform.model.GovernancePolicy;
import com.project.grcplatform.repository.GovernancePolicyRepository;
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
public class GovernanceScheduler {

    private final GovernancePolicyRepository policyRepository;
    private final NotificationService notificationService;

    /**
     * Every day at 08:15 — notify owners of APPROVED policies expiring within 7 days.
     */
    @Scheduled(cron = "0 15 8 * * *")
    public void checkExpiringPolicies() {
        LocalDate threshold = LocalDate.now().plusDays(7);
        List<GovernancePolicy> expiring = policyRepository.findExpiringSoon(threshold);
        log.info("[GovernanceScheduler] Checking expiring policies — found {}", expiring.size());

        for (GovernancePolicy policy : expiring) {
            try {
                notificationService.notify(
                        policy.getCreatedBy() != null ? policy.getCreatedBy().getId() : null,
                        NotificationType.GOVERNANCE_POLICY_EXPIRING,
                        NotificationEntityType.GOVERNANCE_POLICY,
                        policy.getId(),
                        "⚠️ Politique bientôt expirée",
                        "La politique \"" + policy.getTitle() + "\" expire le "
                                + policy.getExpiryDate() + ". Veuillez la renouveler ou la déprécier."
                );
            } catch (Exception e) {
                log.error("[GovernanceScheduler] Failed to notify for policy {}: {}", policy.getId(), e.getMessage());
            }
        }
    }
}