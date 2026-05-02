package com.project.grcplatform.scanner;

import com.project.grcplatform.constant.VulnCriticality;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Nessus REST API adapter.
 *
 * Auth flow:
 *   POST  {baseUrl}/session                     → X-Cookie token
 *   GET   {baseUrl}/scans                       → list of scans
 *   GET   {baseUrl}/scans/{id}                  → scan detail with hosts
 *   GET   {baseUrl}/scans/{id}/hosts/{hostId}   → per-host vulnerabilities
 *
 * Requires: apiKey set as "accessKey:secretKey" in the scanner config,
 * OR username + password for session-based auth.
 *
 * Nessus severity: 0=Info, 1=Low, 2=Medium, 3=High, 4=Critical
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NessusAdapter implements ScannerAdapter {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public List<RawVulnerabilityDTO> fetchVulnerabilities(VulnerabilityScanner config) {
        WebClient client = webClientBuilder.baseUrl(config.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        String token = authenticate(client, config);
        List<RawVulnerabilityDTO> results = new ArrayList<>();

        JsonNode scansResponse = get(client, "/scans", token);
        JsonNode scans = scansResponse.path("scans");
        if (!scans.isArray()) return results;

        for (JsonNode scan : scans) {
            String status = scan.path("status").asText();
            if (!"completed".equalsIgnoreCase(status)) continue;

            int scanId = scan.path("id").asInt();
            JsonNode scanDetail = get(client, "/scans/" + scanId, token);
            JsonNode hosts = scanDetail.path("hosts");
            if (!hosts.isArray()) continue;

            for (JsonNode host : hosts) {
                int hostId   = host.path("host_id").asInt();
                String ip    = host.path("hostname").asText(null);   // Nessus uses "hostname" for the IP
                String hname = host.path("host-fqdn").asText(null);

                JsonNode hostDetail = get(client, "/scans/" + scanId + "/hosts/" + hostId, token);
                JsonNode vulns = hostDetail.path("vulnerabilities");
                if (!vulns.isArray()) continue;

                for (JsonNode vuln : vulns) {
                    int    pluginId  = vuln.path("plugin_id").asInt();
                    String pluginName = vuln.path("plugin_name").asText("Unknown");
                    int    severity  = vuln.path("severity").asInt(0);
                    if (severity == 0) continue; // skip informational

                    String externalRef = scanId + "_" + hostId + "_" + pluginId;

                    // Fetch plugin output for CVSS / CVE details
                    BigDecimal cvss = null;
                    String cveId = null;
                    String description = null;
                    try {
                        JsonNode plugin = get(client,
                                "/scans/" + scanId + "/hosts/" + hostId + "/plugins/" + pluginId, token);
                        JsonNode info = plugin.path("info").path("plugindetails");
                        cvss        = parseCvss(info.path("cvssV3BaseScore").asText(null));
                        description = info.path("description").asText(null);
                        JsonNode cves = info.path("cve");
                        if (cves.isArray() && cves.size() > 0) cveId = cves.get(0).asText();
                    } catch (Exception e) {
                        log.debug("Could not fetch plugin details for pluginId {}: {}", pluginId, e.getMessage());
                    }

                    results.add(RawVulnerabilityDTO.builder()
                            .externalRef(externalRef)
                            .title(pluginName)
                            .description(description)
                            .cvssScore(cvss)
                            .cveId(cveId)
                            .rawSeverity(severity)
                            .targetIp(ip)
                            .targetHostname(hname)
                            .detectedAt(LocalDateTime.now())
                            .build());
                }
            }
        }

        return results;
    }

    /** Returns the X-Cookie session token. Supports both API-key and username/password auth. */
    private String authenticate(WebClient client, VulnerabilityScanner config) {
        // API key auth: apiKey field contains "accessKey:secretKey"
        if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
            // Nessus API key auth uses X-ApiKeys header — return the key directly as marker
            return "apikey:" + config.getApiKey();
        }
        // Session auth
        Map<String, String> body = Map.of(
                "username", config.getUsername(),
                "password", config.getPassword());
        JsonNode response = client.post().uri("/session")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        if (response == null || !response.has("token"))
            throw new IllegalStateException("Nessus authentication failed: no token in response");
        return "session:" + response.get("token").asText();
    }

    private JsonNode get(WebClient client, String uri, String token) {
        WebClient.RequestHeadersSpec<?> spec = client.get().uri(uri);
        if (token.startsWith("apikey:")) {
            String[] parts = token.substring(7).split(":", 2);
            spec = client.get().uri(uri)
                    .header("X-ApiKeys", "accessKey=" + parts[0] + "; secretKey=" + (parts.length > 1 ? parts[1] : ""));
        } else {
            String sessionToken = token.startsWith("session:") ? token.substring(8) : token;
            spec = client.get().uri(uri).header("X-Cookie", "token=" + sessionToken);
        }
        JsonNode result = spec.retrieve().bodyToMono(JsonNode.class).block();
        return result != null ? result : objectMapper.createObjectNode();
    }

    private BigDecimal parseCvss(String value) {
        if (value == null || value.isBlank()) return null;
        try { return new BigDecimal(value); } catch (NumberFormatException e) { return null; }
    }
}
