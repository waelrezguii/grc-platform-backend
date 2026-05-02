package com.project.grcplatform.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class CveItemDTO {
    private String cveId;                  // CVE-2021-44228
    private String description;            // English description from NVD
    private Double cvssScore;              // 0.0 - 10.0
    private String cvssVersion;            // 3.1, 3.0, 2.0
    private String severity;              // NONE, LOW, MEDIUM, HIGH, CRITICAL
    private String attackVector;           // NETWORK, ADJACENT, LOCAL, PHYSICAL
    private String attackComplexity;       // LOW, HIGH
    private String privilegesRequired;    // NONE, LOW, HIGH
    private String userInteraction;        // NONE, REQUIRED
    private String confidentialityImpact; // NONE, LOW, HIGH
    private String integrityImpact;       // NONE, LOW, HIGH
    private String availabilityImpact;    // NONE, LOW, HIGH
    private String vectorString;          // CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H
    private LocalDateTime publishedDate;
    private LocalDateTime lastModifiedDate;
    private java.util.List<String> cwes;  // CWE-79, CWE-89...
    private java.util.List<String> references; // advisory URLs
}