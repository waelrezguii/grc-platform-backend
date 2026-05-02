package com.project.grcplatform.controller;

import com.project.grcplatform.constant.AuditCampaignStatus;
import com.project.grcplatform.dto.*;
import com.project.grcplatform.service.AuditCampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-campaigns")
@RequiredArgsConstructor
public class AuditCampaignController {

    private final AuditCampaignService service;

    // ─── Campaigns ────────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<Page<AuditCampaignResponseDTO>> getAll(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String auditTypeId,
            @RequestParam(required = false) AuditCampaignStatus status,
            @RequestParam(required = false) String auditorId,
            @RequestParam(defaultValue = "false") boolean archived,
            Pageable pageable) {
        return ResponseEntity.ok(service.findAll(title, auditTypeId, status, auditorId, archived, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditCampaignResponseDTO> getById(@PathVariable String id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<?> getHistory(@PathVariable String id) {
        return ResponseEntity.ok(service.getHistory(id));
    }

    @GetMapping("/{id}/checklist")
    public ResponseEntity<List<AuditChecklistItemResponseDTO>> getChecklist(@PathVariable String id) {
        return ResponseEntity.ok(service.getChecklist(id));
    }

    @GetMapping("/{id}/findings")
    public ResponseEntity<List<AuditFindingResponseDTO>> getFindings(@PathVariable String id) {
        return ResponseEntity.ok(service.getFindings(id));
    }

    @GetMapping("/{id}/recommendations")
    public ResponseEntity<List<AuditRecommendationResponseDTO>> getRecommendations(@PathVariable String id) {
        return ResponseEntity.ok(service.getRecommendations(id));
    }

    @GetMapping("/{id}/report")
    public ResponseEntity<AuditReportDTO> getReport(@PathVariable String id) {
        return ResponseEntity.ok(service.generateReport(id));
    }

    @PostMapping
    public ResponseEntity<AuditCampaignResponseDTO> create(@RequestBody @Valid AuditCampaignRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AuditCampaignResponseDTO> update(
            @PathVariable String id,
            @RequestBody AuditCampaignRequestDTO request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<AuditCampaignResponseDTO> start(@PathVariable String id) {
        return ResponseEntity.ok(service.start(id));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<AuditCampaignResponseDTO> submit(@PathVariable String id) {
        return ResponseEntity.ok(service.submit(id));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<AuditCampaignResponseDTO> complete(@PathVariable String id) {
        return ResponseEntity.ok(service.complete(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<AuditCampaignResponseDTO> cancel(@PathVariable String id) {
        return ResponseEntity.ok(service.cancel(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Checklist ────────────────────────────────────────────────────────────────

    @PostMapping("/{campaignId}/checklist")
    public ResponseEntity<AuditChecklistItemResponseDTO> addChecklistItem(
            @PathVariable String campaignId,
            @RequestBody @Valid AuditChecklistItemRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.addChecklistItem(campaignId, request));
    }

    @PatchMapping("/{campaignId}/checklist/{itemId}")
    public ResponseEntity<AuditChecklistItemResponseDTO> updateChecklistItem(
            @PathVariable String campaignId,
            @PathVariable String itemId,
            @RequestBody AuditChecklistItemRequestDTO request) {
        return ResponseEntity.ok(service.updateChecklistItem(campaignId, itemId, request));
    }

    @DeleteMapping("/{campaignId}/checklist/{itemId}")
    public ResponseEntity<Void> deleteChecklistItem(
            @PathVariable String campaignId,
            @PathVariable String itemId) {
        service.deleteChecklistItem(campaignId, itemId);
        return ResponseEntity.noContent().build();
    }
}