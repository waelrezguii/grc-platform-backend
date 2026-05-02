package com.project.grcplatform.scanner;

import com.project.grcplatform.dto.scanner.RawVulnerabilityDTO;
import com.project.grcplatform.model.VulnerabilityScanner;

import java.util.List;

/**
 * Contract that every scanner integration must implement.
 * Each adapter is responsible for authenticating, fetching results,
 * and normalizing them into {@link RawVulnerabilityDTO} objects.
 */
public interface ScannerAdapter {

    List<RawVulnerabilityDTO> fetchVulnerabilities(VulnerabilityScanner config);
}
