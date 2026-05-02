package com.project.grcplatform.controller;

import com.project.grcplatform.constant.LifecycleStatus;
import com.project.grcplatform.dto.AssetIncidentRequestDTO;
import com.project.grcplatform.dto.AssetIncidentResponseDTO;
import com.project.grcplatform.dto.AssetMapDTO;
import com.project.grcplatform.dto.AssetRequestDTO;
import com.project.grcplatform.dto.AssetResponseDTO;
import com.project.grcplatform.model.AssetHistory;
import com.project.grcplatform.service.AssetService;
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
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @GetMapping
    public ResponseEntity<Page<AssetResponseDTO>> findAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String typeId,
            @RequestParam(required = false) LifecycleStatus lifecycleStatus,
            @RequestParam(required = false) String ownerId,
            @RequestParam(required = false) String directionCentraleId,
            @RequestParam(required = false) String directionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return ResponseEntity.ok(assetService.findAll(
                name, categoryId, typeId, lifecycleStatus,
                ownerId, directionCentraleId, directionId,
                PageRequest.of(page, size, sort)));
    }

    @GetMapping("/map")
    public ResponseEntity<AssetMapDTO> getMap(
            @RequestParam(required = false) String directionCentraleId,
            @RequestParam(required = false) String directionId,
            @RequestParam(required = false) String typeId,
            @RequestParam(required = false) String categoryId
    ) {
        return ResponseEntity.ok(assetService.getMap(directionCentraleId, directionId, typeId, categoryId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssetResponseDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(assetService.findById(id));
    }

    @PostMapping
    public ResponseEntity<AssetResponseDTO> create(@Valid @RequestBody AssetRequestDTO request) {
        return ResponseEntity.ok(assetService.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AssetResponseDTO> update(
            @PathVariable String id,
            @RequestBody AssetRequestDTO request
    ) {
        return ResponseEntity.ok(assetService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        assetService.delete(id);
        return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "Asset deleted successfully"));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<AssetHistory>> getHistory(@PathVariable String id) {
        return ResponseEntity.ok(assetService.getHistory(id));
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @PostMapping("/{id}/lifecycle")
    public ResponseEntity<AssetResponseDTO> transition(
            @PathVariable String id,
            @RequestParam LifecycleStatus status
    ) {
        return ResponseEntity.ok(assetService.transition(id, status));
    }

    // ── Expiry / overdue ─────────────────────────────────────────────────────

    @GetMapping("/expiring")
    public ResponseEntity<List<AssetResponseDTO>> getExpiring(
            @RequestParam(defaultValue = "30") int withinDays
    ) {
        return ResponseEntity.ok(assetService.getExpiring(withinDays));
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<AssetResponseDTO>> getOverdue() {
        return ResponseEntity.ok(assetService.getOverdue());
    }

    // ── Incidents ────────────────────────────────────────────────────────────

    @GetMapping("/{id}/incidents")
    public ResponseEntity<List<AssetIncidentResponseDTO>> getIncidents(@PathVariable String id) {
        return ResponseEntity.ok(assetService.getIncidents(id));
    }

    @PostMapping("/{id}/incidents")
    public ResponseEntity<AssetIncidentResponseDTO> createIncident(
            @PathVariable String id,
            @Valid @RequestBody AssetIncidentRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assetService.createIncident(id, request));
    }

    @PatchMapping("/{id}/incidents/{incidentId}")
    public ResponseEntity<AssetIncidentResponseDTO> updateIncident(
            @PathVariable String id,
            @PathVariable String incidentId,
            @RequestBody AssetIncidentRequestDTO request
    ) {
        return ResponseEntity.ok(assetService.updateIncident(id, incidentId, request));
    }

    @DeleteMapping("/{id}/incidents/{incidentId}")
    public ResponseEntity<Void> deleteIncident(
            @PathVariable String id,
            @PathVariable String incidentId
    ) {
        assetService.deleteIncident(id, incidentId);
        return ResponseEntity.noContent().build();
    }
}
