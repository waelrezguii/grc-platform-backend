package com.project.grcplatform.controller;

import com.project.grcplatform.constant.RecommendationPriority;
import com.project.grcplatform.constant.RecommendationStatus;
import com.project.grcplatform.dto.AuditRecommendationRequestDTO;
import com.project.grcplatform.dto.AuditRecommendationResponseDTO;
import com.project.grcplatform.service.AuditRecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit-recommendations")
@RequiredArgsConstructor
public class AuditRecommendationController {

    private final AuditRecommendationService service;

    @GetMapping
    public ResponseEntity<Page<AuditRecommendationResponseDTO>> getAll(
            @RequestParam(required = false) String campaignId,
            @RequestParam(required = false) String findingId,
            @RequestParam(required = false) RecommendationStatus status,
            @RequestParam(required = false) String assigneeId,
            @RequestParam(required = false) RecommendationPriority priority,
            Pageable pageable) {
        return ResponseEntity.ok(service.findAll(campaignId, findingId, status, assigneeId, priority, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditRecommendationResponseDTO> getById(@PathVariable String id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<?> getHistory(@PathVariable String id) {
        return ResponseEntity.ok(service.getHistory(id));
    }

    @PostMapping
    public ResponseEntity<AuditRecommendationResponseDTO> create(
            @RequestBody @Valid AuditRecommendationRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AuditRecommendationResponseDTO> update(
            @PathVariable String id,
            @RequestBody AuditRecommendationRequestDTO request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AuditRecommendationResponseDTO> updateStatus(
            @PathVariable String id,
            @RequestParam RecommendationStatus status) {
        return ResponseEntity.ok(service.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}