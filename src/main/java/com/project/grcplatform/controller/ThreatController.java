package com.project.grcplatform.controller;

import com.project.grcplatform.constant.*;
import com.project.grcplatform.dto.ThreatRequestDTO;
import com.project.grcplatform.dto.ThreatResponseDTO;
import com.project.grcplatform.dto.ThreatReviewRequestDTO;
import com.project.grcplatform.model.ThreatHistory;
import com.project.grcplatform.service.ThreatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/threats")
@RequiredArgsConstructor
public class ThreatController {

    private final ThreatService threatService;

    @GetMapping
    public ResponseEntity<Page<ThreatResponseDTO>> findAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) ThreatOrigin origin,
            @RequestParam(required = false) String typeId,
            @RequestParam(required = false) ThreatSeverity severity,
            @RequestParam(required = false) ThreatStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return ResponseEntity.ok(
                threatService.findAll(name, origin, typeId, severity, status, PageRequest.of(page, size, sort))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ThreatResponseDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(threatService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ThreatResponseDTO> create(@Valid @RequestBody ThreatRequestDTO request) {
        return ResponseEntity.ok(threatService.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ThreatResponseDTO> update(
            @PathVariable String id,
            @RequestBody ThreatRequestDTO request
    ) {
        return ResponseEntity.ok(threatService.update(id, request));
    }

    @PostMapping("/{id}/validate")
    public ResponseEntity<ThreatResponseDTO> validate(@PathVariable String id) {
        return ResponseEntity.ok(threatService.validate(id));
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<ThreatResponseDTO> review(
            @PathVariable String id,
            @Valid @RequestBody ThreatReviewRequestDTO request) {
        return ResponseEntity.ok(threatService.review(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        threatService.delete(id);
        return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "Threat deleted successfully"));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<ThreatHistory>> getHistory(@PathVariable String id) {
        return ResponseEntity.ok(threatService.getHistory(id));
    }
}
