package com.project.grcplatform.controller;

import com.project.grcplatform.dto.*;
import com.project.grcplatform.service.CveImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CveController {

    private final CveImportService cveImportService;

    /**
     * GET /api/cve/search?keyword=log4j&limit=10
     * Search CVEs from NVD by keyword.
     */
    @GetMapping("/api/cve/search")
    public ResponseEntity<CveSearchResponseDTO> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(cveImportService.search(keyword, limit));
    }

    /**
     * GET /api/cve/{cveId}
     * Get full details of a specific CVE from NVD.
     * Example: GET /api/cve/CVE-2021-44228
     */
    @GetMapping("/api/cve/{cveId}")
    public ResponseEntity<CveItemDTO> getCve(@PathVariable String cveId) {
        return ResponseEntity.ok(cveImportService.getCveById(cveId));
    }

    /**
     * POST /api/vulnerabilities/import/cve
     * Import a CVE from NVD as a Vulnerability linked to an asset.
     * Body: { "cveId": "CVE-2021-44228", "assetId": "uuid" }
     */
    @PostMapping("/api/vulnerabilities/import/cve")
    public ResponseEntity<VulnerabilityResponseDTO> importCve(
            @RequestBody @Valid CveImportRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cveImportService.importCve(request));
    }
}