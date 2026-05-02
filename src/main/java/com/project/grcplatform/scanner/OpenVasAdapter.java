package com.project.grcplatform.scanner;

import com.project.grcplatform.dto.scanner.RawVulnerabilityDTO;
import com.project.grcplatform.model.VulnerabilityScanner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * OpenVAS / GVM REST API adapter.
 *
 * Targets the GVM HTTP API exposed by the Greenbone Security Assistant Daemon (gsad).
 * Auth: POST {baseUrl}/api/v1/auth/sign_in  → Bearer token
 * Reports endpoint: GET {baseUrl}/api/v1/reports?filter=min_qod%3D70
 *
 * OpenVAS severity: CVSS score bands — 0.0=Info, 0.1-3.9=Low, 4.0-6.9=Medium, 7.0-8.9=High, 9.0-10=Critical
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenVasAdapter implements ScannerAdapter {

    private static final DateTimeFormatter GVM_FMT = DateTimeFormatter.ISO_DATE_TIME;

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public List<RawVulnerabilityDTO> fetchVulnerabilities(VulnerabilityScanner config) {
        WebClient client = webClientBuilder.baseUrl(config.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        String bearerToken = authenticate(client, config);
        List<RawVulnerabilityDTO> results = new ArrayList<>();

        // Fetch reports list
        JsonNode reportsResponse = client.get()
                .uri("/api/v1/reports?filter=min_qod%3D70&sort_field=date&sort_order=descending")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (reportsResponse == null) return results;

        JsonNode reports = reportsResponse.path("reports");
        if (!reports.isArray()) return results;

        // Process only the most recent completed report to avoid duplicating old data
        for (JsonNode reportSummary : reports) {
            String reportId = reportSummary.path("id").asText(null);
            String scanStatus = reportSummary.path("report").path("scan_run_status").asText("");
            if (!"Done".equalsIgnoreCase(scanStatus) || reportId == null) continue;

            JsonNode reportDetail = client.get()
                    .uri("/api/v1/reports/" + reportId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (reportDetail == null) continue;

            JsonNode vulns = reportDetail.path("report").path("report").path("results").path("result");
            if (!vulns.isArray()) continue;

            for (JsonNode result : vulns) {
                double severity = result.path("severity").asDouble(0.0);
                if (severity <= 0.0) continue; // skip informational

                String nvtOid  = result.path("nvt").path("oid").asText(null);
                String host    = result.path("host").path("text").asText(null);
                String hostname = result.path("host").path("hostname").asText(null);
                String name    = result.path("name").asText("Unknown");
                String desc    = result.path("description").asText(null);
                String cveId   = extractCve(result.path("nvt").path("refs").path("ref"));
                String date    = result.path("modification_time").asText(null);
                String cvssStr = result.path("nvt").path("cvss_base").asText(null);

                String externalRef = "GVM_" + nvtOid + "_" + host;

                results.add(RawVulnerabilityDTO.builder()
                        .externalRef(externalRef)
                        .title(name)
                        .description(desc)
                        .cvssScore(parseBigDecimal(cvssStr))
                        .cveId(cveId)
                        .rawSeverity(cvssToRawSeverity(severity))
                        .targetIp(host)
                        .targetHostname(hostname)
                        .detectedAt(parseDate(date))
                        .build());
            }
            // Only process the latest report
            break;
        }

        return results;
    }

    private String authenticate(WebClient client, VulnerabilityScanner config) {
        Map<String, String> body = Map.of(
                "username", config.getUsername() != null ? config.getUsername() : "",
                "password", config.getPassword() != null ? config.getPassword() : "");
        JsonNode response = client.post()
                .uri("/api/v1/auth/sign_in")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        if (response == null || !response.has("token"))
            throw new IllegalStateException("OpenVAS authentication failed: no token in response");
        return response.get("token").asText();
    }

    /**
     * Maps CVSS score to a 0-4 rawSeverity that matches Nessus convention
     * so the sync service can use the same mapping table.
     */
    private int cvssToRawSeverity(double cvss) {
        if (cvss >= 9.0) return 4;
        if (cvss >= 7.0) return 3;
        if (cvss >= 4.0) return 2;
        return 1;
    }

    private String extractCve(JsonNode refs) {
        if (!refs.isArray()) return null;
        for (JsonNode ref : refs) {
            if ("cve".equalsIgnoreCase(ref.path("type").asText())) {
                return ref.path("id").asText(null);
            }
        }
        return null;
    }

    private BigDecimal parseBigDecimal(String v) {
        if (v == null || v.isBlank()) return null;
        try { return new BigDecimal(v.trim()); } catch (NumberFormatException e) { return null; }
    }

    private LocalDateTime parseDate(String v) {
        if (v == null) return LocalDateTime.now();
        try { return LocalDateTime.parse(v, GVM_FMT); } catch (Exception e) { return LocalDateTime.now(); }
    }
}
