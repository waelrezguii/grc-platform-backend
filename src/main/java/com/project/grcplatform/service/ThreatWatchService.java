package com.project.grcplatform.service;

import com.project.grcplatform.constant.AuditAction;
import com.project.grcplatform.constant.AuditEntityType;
import com.project.grcplatform.constant.ErrorCode;
import com.project.grcplatform.dto.ThreatWatchEntryResponseDTO;
import com.project.grcplatform.dto.ThreatWatchSourceRequestDTO;
import com.project.grcplatform.dto.ThreatWatchSourceResponseDTO;
import com.project.grcplatform.exception.AppException;
import com.project.grcplatform.model.Threat;
import com.project.grcplatform.model.ThreatWatchEntry;
import com.project.grcplatform.model.ThreatWatchSource;
import com.project.grcplatform.repository.ThreatRepository;
import com.project.grcplatform.repository.ThreatWatchEntryRepository;
import com.project.grcplatform.repository.ThreatWatchSourceRepository;
import com.project.grcplatform.security.JwtAuthToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ThreatWatchService {

    private final ThreatWatchSourceRepository sourceRepository;
    private final ThreatWatchEntryRepository entryRepository;
    private final ThreatRepository threatRepository;
    private final AuditService auditService;
    private final WebClient.Builder webClientBuilder;

    // ── Sources ──────────────────────────────────────────────────────────────

    public Page<ThreatWatchSourceResponseDTO> findAllSources(Boolean enabled, Pageable pageable) {
        return sourceRepository.findAllWithFilters(enabled, pageable).map(this::toSourceDTO);
    }

    public ThreatWatchSourceResponseDTO findSourceById(String id) {
        return toSourceDTO(getSourceOrThrow(id));
    }

    @Transactional
    public ThreatWatchSourceResponseDTO createSource(ThreatWatchSourceRequestDTO request) {
        ThreatWatchSource source = ThreatWatchSource.builder()
                .name(request.getName())
                .url(request.getUrl())
                .type(request.getType())
                .description(request.getDescription())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .build();
        ThreatWatchSource saved = sourceRepository.save(source);

        auditService.log(AuditAction.THREAT_WATCH_SOURCE_CREATED, AuditEntityType.THREAT_WATCH_SOURCE,
                saved.getId(), "Watch source created: " + saved.getName());

        return toSourceDTO(saved);
    }

    @Transactional
    public ThreatWatchSourceResponseDTO updateSource(String id, ThreatWatchSourceRequestDTO request) {
        ThreatWatchSource source = getSourceOrThrow(id);
        if (request.getName() != null)        source.setName(request.getName());
        if (request.getUrl() != null)         source.setUrl(request.getUrl());
        if (request.getType() != null)        source.setType(request.getType());
        if (request.getDescription() != null) source.setDescription(request.getDescription());
        if (request.getEnabled() != null)     source.setEnabled(request.getEnabled());

        ThreatWatchSource saved = sourceRepository.save(source);

        auditService.log(AuditAction.THREAT_WATCH_SOURCE_UPDATED, AuditEntityType.THREAT_WATCH_SOURCE,
                saved.getId(), "Watch source updated: " + saved.getName());

        return toSourceDTO(saved);
    }

    @Transactional
    public void deleteSource(String id) {
        ThreatWatchSource source = getSourceOrThrow(id);
        source.setDeleted(true);
        sourceRepository.save(source);

        auditService.log(AuditAction.THREAT_WATCH_SOURCE_DELETED, AuditEntityType.THREAT_WATCH_SOURCE,
                id, "Watch source deleted: " + source.getName());
    }

    // ── Fetch ─────────────────────────────────────────────────────────────────

    /**
     * Fetches a single source and imports new entries.
     * Returns the number of new entries imported.
     */
    @Transactional
    public ThreatWatchSourceResponseDTO fetchSource(String id) {
        ThreatWatchSource source = getSourceOrThrow(id);
        int imported = fetchAndImport(source);
        source.setLastFetchAt(LocalDateTime.now());
        source.setLastFetchCount(imported);
        sourceRepository.save(source);

        auditService.log(AuditAction.THREAT_WATCH_SOURCE_FETCHED, AuditEntityType.THREAT_WATCH_SOURCE,
                id, "Watch source fetched: " + source.getName() + " — " + imported + " new entries");

        return toSourceDTO(source);
    }

    /** Called by scheduler: fetches all enabled sources. */
    @Transactional
    public void fetchAllEnabled() {
        List<ThreatWatchSource> sources = sourceRepository.findByEnabledTrueAndDeletedFalse();
        for (ThreatWatchSource source : sources) {
            try {
                int imported = fetchAndImport(source);
                source.setLastFetchAt(LocalDateTime.now());
                source.setLastFetchCount(imported);
                sourceRepository.save(source);
                log.info("[ThreatWatch] {} — {} new entries", source.getName(), imported);
            } catch (Exception e) {
                log.error("[ThreatWatch] Failed to fetch source '{}': {}", source.getName(), e.getMessage());
            }
        }
    }

    // ── Entries ───────────────────────────────────────────────────────────────

    public Page<ThreatWatchEntryResponseDTO> findAllEntries(String sourceId, Boolean processed, Pageable pageable) {
        return entryRepository.findAllWithFilters(sourceId, processed, pageable).map(this::toEntryDTO);
    }

    /**
     * Marks an entry as processed and optionally links it to an existing threat.
     * Pass {@code linkedThreatId = null} to only mark as processed without linking.
     */
    @Transactional
    public ThreatWatchEntryResponseDTO processEntry(String entryId, String linkedThreatId) {
        ThreatWatchEntry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new AppException(ErrorCode.THREAT_WATCH_ENTRY_NOT_FOUND));

        entry.setProcessed(true);
        entry.setProcessedBy(getCurrentUserId());
        entry.setProcessedAt(LocalDateTime.now());

        if (linkedThreatId != null) {
            Threat threat = threatRepository.findByIdAndDeletedFalse(linkedThreatId)
                    .orElseThrow(() -> new AppException(ErrorCode.THREAT_NOT_FOUND));
            entry.setLinkedThreat(threat);
        }

        ThreatWatchEntry saved = entryRepository.save(entry);

        auditService.log(AuditAction.THREAT_WATCH_ENTRY_PROCESSED, AuditEntityType.THREAT_WATCH_ENTRY,
                entryId, "Watch entry processed: " + entry.getTitle());

        return toEntryDTO(saved);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private int fetchAndImport(ThreatWatchSource source) {
        String xml = fetchUrl(source.getUrl());
        if (xml == null || xml.isBlank()) return 0;

        List<RawFeedItem> items = parseXml(xml);
        int imported = 0;
        for (RawFeedItem item : items) {
            String url = item.url != null ? item.url : "";
            if (url.isBlank() || entryRepository.existsBySource_IdAndEntryUrl(source.getId(), url)) {
                continue; // skip duplicates or entries with no URL
            }
            ThreatWatchEntry entry = ThreatWatchEntry.builder()
                    .source(source)
                    .title(item.title != null ? item.title : "(no title)")
                    .summary(item.summary)
                    .entryUrl(url)
                    .publishedAt(item.publishedAt)
                    .build();
            entryRepository.save(entry);
            imported++;
        }
        return imported;
    }

    private String fetchUrl(String url) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();
        } catch (Exception e) {
            log.warn("[ThreatWatch] HTTP fetch failed for {}: {}", url, e.getMessage());
            return null;
        }
    }

    /**
     * Parses RSS 2.0 {@code <item>} or Atom {@code <entry>} elements from XML.
     */
    private List<RawFeedItem> parseXml(String xml) {
        List<RawFeedItem> results = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));
            doc.getDocumentElement().normalize();

            // RSS 2.0: <item> elements
            NodeList items = doc.getElementsByTagName("item");
            if (items.getLength() > 0) {
                for (int i = 0; i < items.getLength(); i++) {
                    results.add(parseRssItem((Element) items.item(i)));
                }
                return results;
            }

            // Atom: <entry> elements
            NodeList entries = doc.getElementsByTagName("entry");
            for (int i = 0; i < entries.getLength(); i++) {
                results.add(parseAtomEntry((Element) entries.item(i)));
            }
        } catch (Exception e) {
            log.warn("[ThreatWatch] XML parse error: {}", e.getMessage());
        }
        return results;
    }

    private RawFeedItem parseRssItem(Element el) {
        RawFeedItem item = new RawFeedItem();
        item.title   = text(el, "title");
        item.summary = text(el, "description");
        item.url     = text(el, "link");
        String pubDate = text(el, "pubDate");
        if (pubDate != null) item.publishedAt = parseRssDate(pubDate);
        return item;
    }

    private RawFeedItem parseAtomEntry(Element el) {
        RawFeedItem item = new RawFeedItem();
        item.title   = text(el, "title");
        item.summary = text(el, "summary");
        // Atom <link href="..."/>
        NodeList links = el.getElementsByTagName("link");
        if (links.getLength() > 0) {
            Node link = links.item(0);
            if (link instanceof Element linkEl) {
                item.url = linkEl.getAttribute("href");
                if (item.url.isBlank()) item.url = linkEl.getTextContent().trim();
            }
        }
        String updated = text(el, "updated");
        if (updated == null) updated = text(el, "published");
        if (updated != null) item.publishedAt = parseIsoDate(updated);
        return item;
    }

    private String text(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0) return null;
        String value = nodes.item(0).getTextContent();
        return value != null ? value.trim() : null;
    }

    private LocalDateTime parseRssDate(String raw) {
        // RFC 822 format: "Mon, 01 Jan 2024 12:00:00 +0000"
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern(
                    "EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
            return ZonedDateTime.parse(raw.trim(), fmt).toLocalDateTime();
        } catch (DateTimeParseException e) {
            return parseIsoDate(raw);
        }
    }

    private LocalDateTime parseIsoDate(String raw) {
        try {
            return ZonedDateTime.parse(raw.trim()).toLocalDateTime();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static class RawFeedItem {
        String title;
        String summary;
        String url;
        LocalDateTime publishedAt;
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private ThreatWatchSourceResponseDTO toSourceDTO(ThreatWatchSource s) {
        ThreatWatchSourceResponseDTO dto = new ThreatWatchSourceResponseDTO();
        dto.setId(s.getId());
        dto.setName(s.getName());
        dto.setUrl(s.getUrl());
        dto.setType(s.getType());
        dto.setDescription(s.getDescription());
        dto.setEnabled(s.getEnabled());
        dto.setLastFetchAt(s.getLastFetchAt());
        dto.setLastFetchCount(s.getLastFetchCount());
        dto.setCreatedAt(s.getCreatedAt());
        dto.setUpdatedAt(s.getUpdatedAt());
        return dto;
    }

    private ThreatWatchEntryResponseDTO toEntryDTO(ThreatWatchEntry e) {
        ThreatWatchEntryResponseDTO dto = new ThreatWatchEntryResponseDTO();
        dto.setId(e.getId());
        if (e.getSource() != null) {
            dto.setSourceId(e.getSource().getId());
            dto.setSourceName(e.getSource().getName());
        }
        dto.setTitle(e.getTitle());
        dto.setSummary(e.getSummary());
        dto.setEntryUrl(e.getEntryUrl());
        dto.setPublishedAt(e.getPublishedAt());
        dto.setProcessed(e.getProcessed());
        dto.setProcessedBy(e.getProcessedBy());
        dto.setProcessedAt(e.getProcessedAt());
        if (e.getLinkedThreat() != null) {
            dto.setLinkedThreatId(e.getLinkedThreat().getId());
            dto.setLinkedThreatName(e.getLinkedThreat().getName());
        }
        dto.setCreatedAt(e.getCreatedAt());
        return dto;
    }

    private ThreatWatchSource getSourceOrThrow(String id) {
        return sourceRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.THREAT_WATCH_SOURCE_NOT_FOUND));
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthToken jwtAuthToken) return jwtAuthToken.getUserId();
        return "system";
    }
}
