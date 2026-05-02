package com.project.grcplatform.scanner;

import com.project.grcplatform.dto.scanner.RawVulnerabilityDTO;
import com.project.grcplatform.model.VulnerabilityScanner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.xml.sax.SAXException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Qualys VMPC API adapter.
 *
 * Auth: HTTP Basic (username + password).
 * Endpoint: POST {baseUrl}/api/2.0/fo/asset/host/vm/detection/?action=list&show_results=1
 * Response: XML — HOST_LIST_VM_DETECTION_OUTPUT
 *
 * Qualys severity: 1=Minimal, 2=Medium, 3=Serious, 4=Critical, 5=Urgent
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QualysAdapter implements ScannerAdapter {

    private static final DateTimeFormatter QUALYS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private final WebClient.Builder webClientBuilder;

    @Override
    public List<RawVulnerabilityDTO> fetchVulnerabilities(VulnerabilityScanner config) {
        String basicAuth = Base64.getEncoder().encodeToString(
                (config.getUsername() + ":" + config.getPassword()).getBytes(StandardCharsets.UTF_8));

        WebClient client = webClientBuilder.baseUrl(config.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth)
                .defaultHeader("X-Requested-With", "GRC-Platform")
                .build();

        String xml = client.post()
                .uri("/api/2.0/fo/asset/host/vm/detection/?action=list&show_results=1&status=Active")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (xml == null || xml.isBlank()) return List.of();
        return parseXmlResponse(xml);
    }

    private List<RawVulnerabilityDTO> parseXmlResponse(String xml) {
        List<RawVulnerabilityDTO> results = new ArrayList<>();
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setExpandEntityReferences(false);
            Document doc = dbf.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            doc.getDocumentElement().normalize();

            NodeList hosts = doc.getElementsByTagName("HOST");
            for (int h = 0; h < hosts.getLength(); h++) {
                Element host = (Element) hosts.item(h);
                String ip   = text(host, "IP");
                String dns  = text(host, "DNS");

                NodeList detections = host.getElementsByTagName("DETECTION");
                for (int d = 0; d < detections.getLength(); d++) {
                    Element det = (Element) detections.item(d);
                    String qid       = text(det, "QID");
                    String title     = text(det, "RESULTS");   // RESULTS has the finding title in Qualys
                    String type      = text(det, "TYPE");
                    if ("Info".equalsIgnoreCase(type)) continue;

                    int    severity  = parseInt(text(det, "SEVERITY"), 1);
                    String cvssStr   = text(det, "CVSS_FINAL");
                    String cveList   = extractFirstCve(det);
                    String detectedStr = text(det, "FIRST_FOUND_DATETIME");

                    results.add(RawVulnerabilityDTO.builder()
                            .externalRef("QUALYS_" + qid + "_" + ip)
                            .title("QID-" + qid + (title != null ? ": " + truncate(title, 200) : ""))
                            .description(text(det, "DIAGNOSIS"))
                            .cvssScore(parseBigDecimal(cvssStr))
                            .cveId(cveList)
                            .rawSeverity(severity)
                            .targetIp(ip)
                            .targetHostname(dns)
                            .detectedAt(parseDate(detectedStr))
                            .build());
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            log.error("Qualys XML parse error: {}", e.getMessage(), e);
        }
        return results;
    }

    private String text(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0) return null;
        String v = nodes.item(0).getTextContent();
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private String extractFirstCve(Element det) {
        NodeList cves = det.getElementsByTagName("ID");
        return cves.getLength() > 0 ? cves.item(0).getTextContent().trim() : null;
    }

    private int parseInt(String v, int def) {
        if (v == null) return def;
        try { return Integer.parseInt(v.trim()); } catch (NumberFormatException e) { return def; }
    }

    private BigDecimal parseBigDecimal(String v) {
        if (v == null || v.isBlank()) return null;
        try { return new BigDecimal(v.trim()); } catch (NumberFormatException e) { return null; }
    }

    private LocalDateTime parseDate(String v) {
        if (v == null) return LocalDateTime.now();
        try { return LocalDateTime.parse(v.trim(), QUALYS_FMT); } catch (Exception e) { return LocalDateTime.now(); }
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
