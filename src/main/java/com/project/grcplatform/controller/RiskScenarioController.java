package com.project.grcplatform.controller;

import com.project.grcplatform.constant.ScenarioStatus;
import com.project.grcplatform.dto.*;
import com.project.grcplatform.model.RiskScenarioHistory;
import com.project.grcplatform.service.RiskScenarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scenarios")
@RequiredArgsConstructor
public class RiskScenarioController {

    private final RiskScenarioService scenarioService;

    @GetMapping
    public ResponseEntity<Page<RiskScenarioResponseDTO>> findAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String assetId,
            @RequestParam(required = false) String threatId,
            @RequestParam(required = false) ScenarioStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "false") boolean archived
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return ResponseEntity.ok(
                scenarioService.findAll(name, assetId, threatId, status, archived, PageRequest.of(page, size, sort))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<RiskScenarioResponseDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(scenarioService.findById(id));
    }

    @PostMapping
    public ResponseEntity<RiskScenarioResponseDTO> create(@Valid @RequestBody RiskScenarioRequestDTO request) {
        return ResponseEntity.ok(scenarioService.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<RiskScenarioResponseDTO> update(
            @PathVariable String id,
            @RequestBody RiskScenarioRequestDTO request
    ) {
        return ResponseEntity.ok(scenarioService.update(id, request));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<RiskScenarioResponseDTO> submitForReview(@PathVariable String id) {
        return ResponseEntity.ok(scenarioService.submitForReview(id));
    }

    @PostMapping("/{id}/validate")
    public ResponseEntity<RiskScenarioResponseDTO> validate(@PathVariable String id) {
        return ResponseEntity.ok(scenarioService.validate(id));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<RiskScenarioResponseDTO> archive(@PathVariable String id) {
        return ResponseEntity.ok(scenarioService.archive(id));
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<RiskScenarioResponseDTO> review(
            @PathVariable String id,
            @Valid @RequestBody RiskScenarioReviewRequestDTO request) {
        return ResponseEntity.ok(scenarioService.review(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        scenarioService.delete(id);
        return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "Scenario deleted successfully"));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<RiskScenarioHistory>> getHistory(@PathVariable String id) {
        return ResponseEntity.ok(scenarioService.getHistory(id));
    }

    @GetMapping("/{id}/history/{historyId}")
    public ResponseEntity<RiskScenarioHistory> getHistoryEntry(
            @PathVariable String id,
            @PathVariable String historyId) {
        return ResponseEntity.ok(scenarioService.getHistoryEntry(id, historyId));
    }

    // ── Incidents ─────────────────────────────────────────────────────────────

    @GetMapping("/{id}/incidents")
    public ResponseEntity<List<ScenarioIncidentResponseDTO>> getIncidents(@PathVariable String id) {
        return ResponseEntity.ok(scenarioService.getIncidents(id));
    }

    @PostMapping("/{id}/incidents")
    public ResponseEntity<ScenarioIncidentResponseDTO> createIncident(
            @PathVariable String id,
            @Valid @RequestBody ScenarioIncidentRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(scenarioService.createIncident(id, request));
    }

    @PatchMapping("/{id}/incidents/{incidentId}")
    public ResponseEntity<ScenarioIncidentResponseDTO> updateIncident(
            @PathVariable String id,
            @PathVariable String incidentId,
            @RequestBody ScenarioIncidentRequestDTO request) {
        return ResponseEntity.ok(scenarioService.updateIncident(id, incidentId, request));
    }

    @DeleteMapping("/{id}/incidents/{incidentId}")
    public ResponseEntity<?> deleteIncident(
            @PathVariable String id,
            @PathVariable String incidentId) {
        scenarioService.deleteIncident(id, incidentId);
        return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "Incident removed successfully"));
    }
}