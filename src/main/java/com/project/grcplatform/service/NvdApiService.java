package com.project.grcplatform.service;

import com.project.grcplatform.dto.CveItemDTO;
import com.project.grcplatform.dto.CveSearchResponseDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("SpellCheckingInspection")
@Service
@RequiredArgsConstructor
@Slf4j
public class NvdApiService {

    private static final String NVD_BASE_URL = "https://services.nvd.nist.gov/rest/json/cves/2.0";
    private static final DateTimeFormatter NVD_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    // Optional NVD API key — higher rate limits (50 req/30s vs 5 req/30s without key)
    @Value("${nvd.api.key:}")
    private String nvdApiKey;

    // ─── Search by keyword ────────────────────────────────────────────────────

    public CveSearchResponseDTO searchByKeyword(String keyword, int limit) {
        String url = UriComponentsBuilder.fromUriString(NVD_BASE_URL)
                .queryParam("keywordSearch", keyword)
                .queryParam("resultsPerPage", Math.min(limit, 50))
                .toUriString();

        return fetchAndParse(url);
    }

    // ─── Fetch by CVE ID ──────────────────────────────────────────────────────

    public Optional<CveItemDTO> fetchByCveId(String cveId) {
        String url = UriComponentsBuilder.fromUriString(NVD_BASE_URL)
                .queryParam("cveId", cveId.toUpperCase())
                .toUriString();

        CveSearchResponseDTO result = fetchAndParse(url);

        if (result.getVulnerabilities() == null || result.getVulnerabilities().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(result.getVulnerabilities().get(0));
    }

    // ─── Core fetch + parse ───────────────────────────────────────────────────

    private CveSearchResponseDTO fetchAndParse(String url) {
        try {
            WebClient client = webClientBuilder
                    .baseUrl(NVD_BASE_URL)
                    .defaultHeader("Accept", "application/json")
                    .build();

            WebClient.RequestHeadersSpec<?> request = client.get().uri(url);

            // Add API key if configured
            if (nvdApiKey != null && !nvdApiKey.isBlank()) {
                request = client.get().uri(url)
                        .header("apiKey", nvdApiKey);
            }

            String responseBody = request
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseNvdResponse(responseBody);

        } catch (Exception e) {
            log.error("[NvdApiService] Failed to fetch from NVD: {}", e.getMessage());
            return CveSearchResponseDTO.builder()
                    .totalResults(0)
                    .resultsPerPage(0)
                    .startIndex(0)
                    .vulnerabilities(List.of())
                    .build();
        }
    }

    // ─── Parse NVD JSON response ──────────────────────────────────────────────

    private CveSearchResponseDTO parseNvdResponse(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);

        int totalResults    = root.path("totalResults").asInt(0);
        int resultsPerPage  = root.path("resultsPerPage").asInt(0);
        int startIndex      = root.path("startIndex").asInt(0);

        List<CveItemDTO> items = new ArrayList<>();
        JsonNode vulns = root.path("vulnerabilities");

        if (vulns.isArray()) {
            for (JsonNode entry : vulns) {
                JsonNode cveNode = entry.path("cve");
                CveItemDTO item = parseCveNode(cveNode);
                if (item != null) items.add(item);
            }
        }

        return CveSearchResponseDTO.builder()
                .totalResults(totalResults)
                .resultsPerPage(resultsPerPage)
                .startIndex(startIndex)
                .vulnerabilities(items)
                .build();
    }

    private CveItemDTO parseCveNode(JsonNode cve) {
        if (cve == null || cve.isMissingNode()) return null;

        String cveId = cve.path("id").asText(null);

        // ── Description (English) ─────────────────────────────────────────────
        String description = null;
        JsonNode descs = cve.path("descriptions");
        if (descs.isArray()) {
            for (JsonNode d : descs) {
                if ("en".equals(d.path("lang").asText())) {
                    description = d.path("value").asText(null);
                    break;
                }
            }
        }

        // ── CVSS Metrics (try v3.1 first, then v3.0, then v2.0) ──────────────
        Double cvssScore       = null;
        String cvssVersion     = null;
        String severity        = null;
        String attackVector    = null;
        String attackComplexity = null;
        String privRequired    = null;
        String userInteraction = null;
        String confImpact      = null;
        String integrityImpact = null;
        String availImpact     = null;
        String vectorString    = null;

        JsonNode metrics = cve.path("metrics");

        JsonNode cvssNode = null;
        if (metrics.has("cvssMetricV31") && metrics.path("cvssMetricV31").isArray()
                && !metrics.path("cvssMetricV31").isEmpty()) {
            cvssNode = metrics.path("cvssMetricV31").get(0).path("cvssData");
            cvssVersion = "3.1";
        } else if (metrics.has("cvssMetricV30") && metrics.path("cvssMetricV30").isArray()
                && !metrics.path("cvssMetricV30").isEmpty()) {
            cvssNode = metrics.path("cvssMetricV30").get(0).path("cvssData");
            cvssVersion = "3.0";
        } else if (metrics.has("cvssMetricV2") && metrics.path("cvssMetricV2").isArray()
                && !metrics.path("cvssMetricV2").isEmpty()) {
            cvssNode = metrics.path("cvssMetricV2").get(0).path("cvssData");
            cvssVersion = "2.0";
        }

        if (cvssNode != null && !cvssNode.isMissingNode()) {
            cvssScore       = cvssNode.path("baseScore").asDouble(0.0);
            severity        = cvssNode.path("baseSeverity").asText(null);
            attackVector    = cvssNode.path("attackVector").asText(null);
            attackComplexity = cvssNode.path("attackComplexity").asText(null);
            privRequired    = cvssNode.path("privilegesRequired").asText(null);
            userInteraction = cvssNode.path("userInteraction").asText(null);
            confImpact      = cvssNode.path("confidentialityImpact").asText(null);
            integrityImpact = cvssNode.path("integrityImpact").asText(null);
            availImpact     = cvssNode.path("availabilityImpact").asText(null);
            vectorString    = cvssNode.path("vectorString").asText(null);
        }

        // ── CWE IDs ───────────────────────────────────────────────────────────
        List<String> cwes = new ArrayList<>();
        JsonNode weaknesses = cve.path("weaknesses");
        if (weaknesses.isArray()) {
            for (JsonNode w : weaknesses) {
                JsonNode wDescs = w.path("description");
                if (wDescs.isArray()) {
                    for (JsonNode wd : wDescs) {
                        if ("en".equals(wd.path("lang").asText())) {
                            String cweValue = wd.path("value").asText(null);
                            if (cweValue != null && cweValue.startsWith("CWE-")) {
                                cwes.add(cweValue);
                            }
                        }
                    }
                }
            }
        }

        // ── References ────────────────────────────────────────────────────────
        List<String> references = new ArrayList<>();
        JsonNode refs = cve.path("references");
        if (refs.isArray()) {
            for (JsonNode r : refs) {
                String refUrl = r.path("url").asText(null);
                if (refUrl != null) references.add(refUrl);
            }
        }

        // ── Dates ─────────────────────────────────────────────────────────────
        LocalDateTime published = parseDate(cve.path("published").asText(null));
        LocalDateTime modified  = parseDate(cve.path("lastModified").asText(null));

        return CveItemDTO.builder()
                .cveId(cveId)
                .description(description)
                .cvssScore(cvssScore)
                .cvssVersion(cvssVersion)
                .severity(severity)
                .attackVector(attackVector)
                .attackComplexity(attackComplexity)
                .privilegesRequired(privRequired)
                .userInteraction(userInteraction)
                .confidentialityImpact(confImpact)
                .integrityImpact(integrityImpact)
                .availabilityImpact(availImpact)
                .vectorString(vectorString)
                .publishedDate(published)
                .lastModifiedDate(modified)
                .cwes(cwes)
                .references(references)
                .build();
    }

    private LocalDateTime parseDate(String date) {
        if (date == null || date.isBlank()) return null;
        try {
            // NVD format: 2021-12-10T10:15:09.143
            String cleaned = date.length() > 23 ? date.substring(0, 23) : date;
            return LocalDateTime.parse(cleaned, NVD_DATE_FORMAT);
        } catch (Exception e) {
            return null;
        }
    }
}