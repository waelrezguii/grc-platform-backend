package com.project.grcplatform.scheduler;

import com.project.grcplatform.service.ArchiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArchiveScheduler {

    private final ArchiveService archiveService;

    /** Runs every day at 03:00 — applies all enabled archive rules. */
    @Scheduled(cron = "0 0 3 * * *")
    public void dailyArchive() {
        log.info("ArchiveScheduler: starting daily archive run");
        archiveService.runAll();
    }
}
