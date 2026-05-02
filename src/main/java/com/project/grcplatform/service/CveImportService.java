package com.project.grcplatform.service;

import com.project.grcplatform.constant.*;
import com.project.grcplatform.dto.*;
import com.project.grcplatform.exception.AppException;
import com.project.grcplatform.mapper.VulnerabilityMapper;
import com.project.grcplatform.model.*;
import com.project.grcplatform.repository.*;

import java.math.BigDecimal;
import com.project.grcplatform.security.JwtAuthToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings("SpellCheckingInspection")
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CveImportService {

    private final NvdApiService          nvdApiService;
    private final AssetRepository        assetRepository;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final UserRepository         userRepository;
    private final AuditService           auditService;
    private final NotificationService    notificationService;

    // ─── Search CVE by keyword ────────────────────────────────────────────────

    public CveSearchResponseDTO search(String keyword, int limit) {
        log.info("[CveImportService] Searching NVD for keyword: {}", keyword.replaceAll("[\r\n]", "_"));
        return nvdApiService.searchByKeyword(keyword, limit);
    }

    // ─── Get single CVE detail ────────────────────────────────────────────────

    public CveItemDTO getCveById(String cveId) {
        log.info("[CveImportService] Fetching CVE: {}", cveId.replaceAll("[\r\n]", "_"));
        return nvdApiService.fetchByCveId(cveId.toUpperCase())
                .orElseThrow(() -> new AppException(ErrorCode.CVE_NOT_FOUND));
    }

    // ─── Import CVE as Vulnerability ─────────────────────────────────────────

    @Transactional
    public VulnerabilityResponseDTO importCve(CveImportRequestDTO request) {
        String cveId = request.getCveId().toUpperCase();
        log.info("[CveImportService] Importing CVE {} for asset {}", cveId, request.getAssetId());

        // 1. Validate asset exists
        Asset asset = assetRepository.findByIdAndDeletedFalse(request.getAssetId())
                .orElseThrow(() -> new AppException(ErrorCode.ASSET_NOT_FOUND));

        // 2. Check if CVE already imported for this asset
        if (vulnerabilityRepository.existsByCveIdAndAssetIdAndDeletedFalse(cveId, asset.getId())) {
            throw new AppException(ErrorCode.CVE_ALREADY_IMPORTED);
        }

        // 3. Fetch CVE from NVD
        CveItemDTO cve = nvdApiService.fetchByCveId(cveId)
                .orElseThrow(() -> new AppException(ErrorCode.CVE_NOT_FOUND));

        // 4. Map CVE → Vulnerability
        String userId = getCurrentUserId();
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        VulnCriticality criticality = mapSeverityToCriticality(cve.getSeverity(), cve.getCvssScore());

        Vulnerability vulnerability = Vulnerability.builder()
                .title(cveId + (cve.getDescription() != null
                        ? " — " + truncate(cve.getDescription())
                        : ""))
                .description(cve.getDescription())
                .asset(asset)
                .source(VulnSource.CVE)
                .criticality(criticality)
                .cvssScore(cve.getCvssScore() != null ? BigDecimal.valueOf(cve.getCvssScore()) : null)
                .cveId(cveId)
                .status(VulnStatus.OPEN)
                .owner(owner)
                .build();

        Vulnerability saved = vulnerabilityRepository.saveAndFlush(vulnerability);
        Vulnerability fresh = vulnerabilityRepository.findByIdAndDeletedFalse(saved.getId()).orElse(saved);

        // 5. Audit log
        auditService.log(
                AuditAction.VULNERABILITY_CREATED,
                AuditEntityType.VULNERABILITY,
                fresh.getId(),
                "CVE imported from NVD: " + cveId + " on asset: " + asset.getName()
        );

        // 6. Notify asset owner if HIGH or CRITICAL
        if (criticality == VulnCriticality.HIGH || criticality == VulnCriticality.CRITICAL) {
            if (asset.getCreatedBy() != null) {
                notificationService.notify(
                        asset.getCreatedBy().getId(),
                        NotificationType.VULNERABILITY_HIGH_CRITICALITY,
                        NotificationEntityType.VULNERABILITY,
                        fresh.getId(),
                        criticality.name() + " vulnerability imported",
                        "CVE " + cveId + " (" + criticality.name() + ", CVSS: " + cve.getCvssScore()
                                + ") was imported on asset \"" + asset.getName() + "\"."
                );
            }
        }

        log.info("[CveImportService] CVE {} imported successfully as vulnerability {}", cveId, fresh.getId());
        return VulnerabilityMapper.toDTO(fresh);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Map NVD severity string + CVSS score to VulnCriticality enum.
     * Severity string from NVD: NONE, LOW, MEDIUM, HIGH, CRITICAL
     * Fallback: derive from score if severity is null
     */
    private VulnCriticality mapSeverityToCriticality(String severity, Double cvssScore) {
        if (severity != null) {
            return switch (severity.toUpperCase()) {
                case "CRITICAL"        -> VulnCriticality.CRITICAL;
                case "HIGH"            -> VulnCriticality.HIGH;
                case "LOW", "NONE"     -> VulnCriticality.LOW;
                default                -> VulnCriticality.MEDIUM;
            };
        }
        // Fallback: derive from CVSS score
        if (cvssScore == null) return VulnCriticality.MEDIUM;
        if (cvssScore >= 9.0) return VulnCriticality.CRITICAL;
        if (cvssScore >= 7.0) return VulnCriticality.HIGH;
        if (cvssScore >= 4.0) return VulnCriticality.MEDIUM;
        return VulnCriticality.LOW;
    }

    private static final int TITLE_MAX_LENGTH = 100;

    private String truncate(String text) {
        if (text == null) return null;
        return text.length() <= TITLE_MAX_LENGTH ? text : text.substring(0, TITLE_MAX_LENGTH) + "...";
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthToken j) return j.getUserId();
        return "system";
    }
}