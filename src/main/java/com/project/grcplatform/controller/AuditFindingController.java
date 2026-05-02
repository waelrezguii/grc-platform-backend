package com.project.grcplatform.controller;

import com.project.grcplatform.constant.FindingSeverity;
import com.project.grcplatform.constant.FindingStatus;
import com.project.grcplatform.constant.FindingType;
import com.project.grcplatform.dto.AuditFindingRequestDTO;
import com.project.grcplatform.dto.AuditFindingResponseDTO;
import com.project.grcplatform.service.AuditFindingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit-findings")
@RequiredArgsConstructor
public class AuditFindingController {

    private final AuditFindingService service;

    @GetMapping
    public ResponseEntity<Page<AuditFindingResponseDTO>> getAll(
            @RequestParam(required = false) String campaignId,
            @RequestParam(required = false) FindingType findingType,
            @RequestParam(required = false) FindingSeverity severity,
            @RequestParam(required = false) FindingStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(service.findAll(campaignId, findingType, severity, status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditFindingResponseDTO> getById(@PathVariable String id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<?> getHistory(@PathVariable String id) {
        return ResponseEntity.ok(service.getHistory(id));
    }

    @PostMapping
    public ResponseEntity<AuditFindingResponseDTO> create(@RequestBody @Valid AuditFindingRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AuditFindingResponseDTO> update(
            @PathVariable String id,
            @RequestBody AuditFindingRequestDTO request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AuditFindingResponseDTO> updateStatus(
            @PathVariable String id,
            @RequestParam FindingStatus status) {
        return ResponseEntity.ok(service.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}