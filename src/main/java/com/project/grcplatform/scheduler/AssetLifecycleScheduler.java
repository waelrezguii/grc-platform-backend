package com.project.grcplatform.scheduler;

import com.project.grcplatform.model.Asset;
import com.project.grcplatform.model.User;
import com.project.grcplatform.repository.AssetRepository;
import com.project.grcplatform.repository.UserRepository;
import com.project.grcplatform.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssetLifecycleScheduler {

    private final AssetRepository assetRepository;
    private final UserRepository  userRepository;
    private final EmailService    emailService;

    /** Runs every day at 08:00. Alerts owners of assets expiring within 30 days or already overdue. */
    @Scheduled(cron = "0 0 8 * * *")
    public void sendEndOfLifeAlerts() {
        LocalDate today   = LocalDate.now();
        LocalDate horizon = today.plusDays(30);

        // Assets expiring within the next 30 days
        List<Asset> expiring = assetRepository.findAllByEndOfLifeDateBetweenAndDeletedFalse(today, horizon);

        // Assets already past end-of-life
        List<Asset> overdue = assetRepository.findAllByEndOfLifeDateBeforeAndDeletedFalse(today);

        int sent = 0;

        for (Asset asset : expiring) {
            User owner = resolveOwner(asset);
            if (owner == null) continue;
            long daysRemaining = ChronoUnit.DAYS.between(today, asset.getEndOfLifeDate());
            try {
                emailService.sendEndOfLifeAlert(owner, asset.getRef(), asset.getName(),
                        asset.getEndOfLifeDate(), (int) daysRemaining);
                sent++;
            } catch (Exception e) {
                log.warn("Failed to send end-of-life alert for asset {} to {}: {}",
                        asset.getRef(), owner.getEmail(), e.getMessage());
            }
        }

        for (Asset asset : overdue) {
            User owner = resolveOwner(asset);
            if (owner == null) continue;
            try {
                emailService.sendEndOfLifeAlert(owner, asset.getRef(), asset.getName(),
                        asset.getEndOfLifeDate(), 0);
                sent++;
            } catch (Exception e) {
                log.warn("Failed to send overdue alert for asset {} to {}: {}",
                        asset.getRef(), owner.getEmail(), e.getMessage());
            }
        }

        log.info("AssetLifecycleScheduler: sent {} end-of-life alert(s) ({} expiring, {} overdue)",
                sent, expiring.size(), overdue.size());
    }

    private User resolveOwner(Asset asset) {
        if (asset.getOwner() != null) return asset.getOwner();
        // owner lazy-loaded; fall back to DB lookup shouldn't normally be needed
        log.debug("Asset {} has no owner loaded, skipping alert", asset.getRef());
        return null;
    }
}
