package com.project.grcplatform.controller;

import com.project.grcplatform.dto.ThreatWatchEntryResponseDTO;
import com.project.grcplatform.dto.ThreatWatchSourceRequestDTO;
import com.project.grcplatform.dto.ThreatWatchSourceResponseDTO;
import com.project.grcplatform.service.ThreatWatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/threat-watch")
@RequiredArgsConstructor
public class ThreatWatchController {

    private final ThreatWatchService watchService;

    // ── Sources ───────────────────────────────────────────────────────────────

    @GetMapping("/sources")
    public ResponseEntity<Page<ThreatWatchSourceResponseDTO>> findAllSources(
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return ResponseEntity.ok(watchService.findAllSources(enabled, PageRequest.of(page, size, sort)));
    }

    @GetMapping("/sources/{id}")
    public ResponseEntity<ThreatWatchSourceResponseDTO> findSourceById(@PathVariable String id) {
        return ResponseEntity.ok(watchService.findSourceById(id));
    }

    @PostMapping("/sources")
    public ResponseEntity<ThreatWatchSourceResponseDTO> createSource(
            @Valid @RequestBody ThreatWatchSourceRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(watchService.createSource(request));
    }

    @PatchMapping("/sources/{id}")
    public ResponseEntity<ThreatWatchSourceResponseDTO> updateSource(
            @PathVariable String id,
            @RequestBody ThreatWatchSourceRequestDTO request) {
        return ResponseEntity.ok(watchService.updateSource(id, request));
    }

    @DeleteMapping("/sources/{id}")
    public ResponseEntity<?> deleteSource(@PathVariable String id) {
        watchService.deleteSource(id);
        return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "Watch source deleted successfully"));
    }

    /** Manual fetch trigger — fetches the source immediately and returns the updated source. */
    @PostMapping("/sources/{id}/fetch")
    public ResponseEntity<ThreatWatchSourceResponseDTO> fetchSource(@PathVariable String id) {
        return ResponseEntity.ok(watchService.fetchSource(id));
    }

    // ── Entries ───────────────────────────────────────────────────────────────

    @GetMapping("/entries")
    public ResponseEntity<Page<ThreatWatchEntryResponseDTO>> findAllEntries(
            @RequestParam(required = false) String sourceId,
            @RequestParam(required = false) Boolean processed,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return ResponseEntity.ok(
                watchService.findAllEntries(sourceId, processed, PageRequest.of(page, size, sort))
        );
    }

    /**
     * Mark an entry as processed and optionally link it to a threat.
     * Body: {@code { "linkedThreatId": "..." }} — omit or set to null to process without linking.
     */
    @PatchMapping("/entries/{id}/process")
    public ResponseEntity<ThreatWatchEntryResponseDTO> processEntry(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        String linkedThreatId = body != null ? body.get("linkedThreatId") : null;
        return ResponseEntity.ok(watchService.processEntry(id, linkedThreatId));
    }
}
