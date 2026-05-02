package com.project.grcplatform.service;

import com.project.grcplatform.constant.ScannerType;
import com.project.grcplatform.constant.SyncStatus;
import com.project.grcplatform.constant.VulnCriticality;
import com.project.grcplatform.constant.VulnSource;
import com.project.grcplatform.constant.VulnStatus;
import com.project.grcplatform.dto.scanner.RawVulnerabilityDTO;
import com.project.grcplatform.model.Asset;
import com.project.grcplatform.model.ScannerSyncLog;
import com.project.grcplatform.model.Vulnerability;
import com.project.grcplatform.model.VulnerabilityScanner;
import com.project.grcplatform.repository.AssetRepository;
import com.project.grcplatform.repository.ScannerSyncLogRepository;
import com.project.grcplatform.repository.UserRepository;
import com.project.grcplatform.repository.VulnerabilityRepository;
import com.project.grcplatform.repository.VulnerabilityScannerRepository;
import com.project.grcplatform.scanner.NessusAdapter;
import com.project.grcplatform.scanner.OpenVasAdapter;
import com.project.grcplatform.scanner.QualysAdapter;
import com.project.grcplatform.scanner.ScannerAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScannerSyncService {

    private final VulnerabilityScannerRepository scannerRepository;
    private final ScannerSyncLogRepository       syncLogRepository;
    private final VulnerabilityRepository        vulnerabilityRepository;
    private final AssetRepository                assetRepository;
    private final UserRepository                 userRepository;

    private final NessusAdapter   nessusAdapter;
    private final QualysAdapter   qualysAdapter;
    private final OpenVasAdapter  openVasAdapter;

    private final EmailService emailService;

    // ── Public API ────────────────────────────────────────────────────────────

    /** Called by the weekly scheduler — syncs all enabled scanners. */
    public void syncAll() {
        List<VulnerabilityScanner> scanners = scannerRepository.findAllByEnabledTrue();
        log.info("Starting scheduled sync for {} enabled scanner(s)", scanners.size());
        for (VulnerabilityScanner scanner : scanners) {
            syncOne(scanner);
        }
    }

    /** Manual trigger — syncs a single scanner by its DB id. */
    @Transactional
    public ScannerSyncLog syncById(String scannerId) {
        VulnerabilityScanner scanner = scannerRepository.findById(scannerId)
                .orElseThrow(() -> new IllegalArgumentException("Scanner not found: " + scannerId));
        return syncOne(scanner);
    }

    // ── Core sync logic ───────────────────────────────────────────────────────

    @Transactional
    public ScannerSyncLog syncOne(VulnerabilityScanner scanner) {
        ScannerSyncLog syncLog = syncLogRepository.save(ScannerSyncLog.builder()
                .scanner(scanner)
                .startedAt(LocalDateTime.now())
                .status(SyncStatus.RUNNING)
                .build());

        List<RawVulnerabilityDTO> raw;
        try {
            ScannerAdapter adapter = resolveAdapter(scanner.getType());
            raw = adapter.fetchVulnerabilities(scanner);
        } catch (Exception e) {
            log.error("Scanner [{}] fetch failed: {}", scanner.getName(), e.getMessage(), e);
            finishLog(syncLog, SyncStatus.FAILED, 0, 0, 0, 0, e.getMessage());
            alertAdmins(scanner, e.getMessage());
            return syncLog;
        }

        int imported = 0, updated = 0, skipped = 0;

        for (RawVulnerabilityDTO raw1 : raw) {
            try {
                Optional<Vulnerability> existing = vulnerabilityRepository
                        .findByExternalRefAndScannerIdAndDeletedFalse(raw1.getExternalRef(), scanner.getId());

                if (existing.isPresent()) {
                    updateVulnerability(existing.get(), raw1, scanner);
                    updated++;
                } else {
                    Optional<Asset> asset = resolveAsset(raw1);
                    if (asset.isEmpty()) {
                        log.debug("No asset match for IP={} hostname={} — skipping",
                                raw1.getTargetIp(), raw1.getTargetHostname());
                        skipped++;
                        continue;
                    }
                    importVulnerability(raw1, asset.get(), scanner);
                    imported++;
                }
            } catch (Exception e) {
                log.warn("Failed to process finding [{}]: {}", raw1.getExternalRef(), e.getMessage());
                skipped++;
            }
        }

        SyncStatus finalStatus = raw.isEmpty()          ? SyncStatus.SUCCESS
                               : (skipped == raw.size()) ? SyncStatus.PARTIAL
                                                         : SyncStatus.SUCCESS;

        finishLog(syncLog, finalStatus, raw.size(), imported, updated, skipped, null);

        scanner.setLastSyncAt(LocalDateTime.now());
        scannerRepository.save(scanner);

        log.info("Sync [{}] done — total={} imported={} updated={} skipped={}",
                scanner.getName(), raw.size(), imported, updated, skipped);
        return syncLog;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ScannerAdapter resolveAdapter(ScannerType type) {
        return switch (type) {
            case NESSUS  -> nessusAdapter;
            case QUALYS  -> qualysAdapter;
            case OPENVAS -> openVasAdapter;
        };
    }

    private Optional<Asset> resolveAsset(RawVulnerabilityDTO raw) {
        // 1. Try exact IP match
        if (raw.getTargetIp() != null && !raw.getTargetIp().isBlank()) {
            Optional<Asset> byIp = assetRepository.findFirstByIpAddressAndDeletedFalse(raw.getTargetIp());
            if (byIp.isPresent()) return byIp;
        }
        // 2. Try hostname match (case-insensitive)
        if (raw.getTargetHostname() != null && !raw.getTargetHostname().isBlank()) {
            return assetRepository.findFirstByHostnameIgnoreCaseAndDeletedFalse(raw.getTargetHostname());
        }
        return Optional.empty();
    }

    private void importVulnerability(RawVulnerabilityDTO raw, Asset asset, VulnerabilityScanner scanner) {
        vulnerabilityRepository.save(Vulnerability.builder()
                .title(raw.getTitle())
                .description(raw.getDescription())
                .source(VulnSource.SCAN)
                .criticality(mapCriticality(raw.getRawSeverity(), scanner.getType()))
                .status(VulnStatus.OPEN)
                .cvssScore(raw.getCvssScore())
                .cveId(raw.getCveId())
                .asset(asset)
                .detectedAt(raw.getDetectedAt() != null ? raw.getDetectedAt() : LocalDateTime.now())
                .externalRef(raw.getExternalRef())
                .scannerId(scanner.getId())
                .lastSeenAt(LocalDateTime.now())
                .build());
    }

    private void updateVulnerability(Vulnerability v, RawVulnerabilityDTO raw, VulnerabilityScanner scanner) {
        v.setCriticality(mapCriticality(raw.getRawSeverity(), scanner.getType()));
        if (raw.getCvssScore() != null)  v.setCvssScore(raw.getCvssScore());
        if (raw.getDescription() != null) v.setDescription(raw.getDescription());
        v.setLastSeenAt(LocalDateTime.now());
        vulnerabilityRepository.save(v);
    }

    /**
     * Maps raw integer severity to VulnCriticality.
     * Nessus:  1=Low, 2=Medium, 3=High, 4=Critical
     * Qualys:  1-2=LOW, 3=MEDIUM, 4=HIGH, 5=CRITICAL
     * OpenVAS: already normalised to 1-4 by the adapter
     */
    private VulnCriticality mapCriticality(int rawSeverity, ScannerType type) {
        if (type == ScannerType.QUALYS) {
            return switch (rawSeverity) {
                case 1, 2 -> VulnCriticality.LOW;
                case 3    -> VulnCriticality.MEDIUM;
                case 4    -> VulnCriticality.HIGH;
                default   -> VulnCriticality.CRITICAL;
            };
        }
        // Nessus + OpenVAS (already normalised to 1-4)
        return switch (rawSeverity) {
            case 1 -> VulnCriticality.LOW;
            case 2 -> VulnCriticality.MEDIUM;
            case 3 -> VulnCriticality.HIGH;
            default -> VulnCriticality.CRITICAL;
        };
    }

    private void finishLog(ScannerSyncLog log, SyncStatus status, int total,
                           int imported, int updated, int skipped, String error) {
        log.setStatus(status);
        log.setCompletedAt(LocalDateTime.now());
        log.setTotalFound(total);
        log.setImported(imported);
        log.setUpdated(updated);
        log.setSkipped(skipped);
        log.setErrorMessage(error);
        syncLogRepository.save(log);
    }

    private void alertAdmins(VulnerabilityScanner scanner, String error) {
        try {
            userRepository.findByRole_Name("ADMIN").forEach(admin ->
                    emailService.sendSyncFailureAlert(admin, scanner.getName(), scanner.getType().name(), error));
        } catch (Exception e) {
            log.warn("Could not send sync failure alert: {}", e.getMessage());
        }
    }
}
