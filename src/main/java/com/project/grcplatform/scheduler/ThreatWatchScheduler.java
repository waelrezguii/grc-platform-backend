package com.project.grcplatform.scheduler;

import com.project.grcplatform.service.ThreatWatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThreatWatchScheduler {

    private final ThreatWatchService threatWatchService;

    /** Fetches all enabled threat watch sources every day at 06:00. */
    @Scheduled(cron = "0 0 6 * * *")
    public void fetchAllSources() {
        log.info("[ThreatWatchScheduler] Starting daily threat watch fetch");
        try {
            threatWatchService.fetchAllEnabled();
            log.info("[ThreatWatchScheduler] Daily threat watch fetch completed");
        } catch (Exception e) {
            log.error("[ThreatWatchScheduler] Fetch failed: {}", e.getMessage(), e);
        }
    }
}
