package com.project.grcplatform.controller;

import com.project.grcplatform.constant.EvaluationStatus;
import com.project.grcplatform.dto.*;
import com.project.grcplatform.model.ComplianceEvaluationHistory;
import com.project.grcplatform.service.ComplianceEvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/compliance/evaluations")
@RequiredArgsConstructor
public class ComplianceEvaluationController {

    private final ComplianceEvaluationService evaluationService;

    @GetMapping
    public ResponseEntity<Page<ComplianceEvaluationResponseDTO>> findAll(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String frameworkId,
            @RequestParam(required = false) EvaluationStatus status,
            @RequestParam(required = false) String evaluatorId,
            @RequestParam(defaultValue = "false") boolean archived,
            Pageable pageable) {
        return ResponseEntity.ok(evaluationService.findAll(title, frameworkId, status, evaluatorId, archived, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComplianceEvaluationResponseDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(evaluationService.findById(id));
    }

    @GetMapping("/{id}/items")
    public ResponseEntity<List<ComplianceEvaluationItemResponseDTO>> getItems(@PathVariable String id) {
        return ResponseEntity.ok(evaluationService.getItems(id));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<ComplianceEvaluationHistory>> getHistory(@PathVariable String id) {
        return ResponseEntity.ok(evaluationService.getHistory(id));
    }

    @PostMapping
    public ResponseEntity<ComplianceEvaluationResponseDTO> create(
            @Valid @RequestBody ComplianceEvaluationRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(evaluationService.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ComplianceEvaluationResponseDTO> update(
            @PathVariable String id, @RequestBody ComplianceEvaluationRequestDTO request) {
        return ResponseEntity.ok(evaluationService.update(id, request));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<ComplianceEvaluationResponseDTO> start(@PathVariable String id) {
        return ResponseEntity.ok(evaluationService.start(id));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ComplianceEvaluationResponseDTO> complete(@PathVariable String id) {
        return ResponseEntity.ok(evaluationService.complete(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ComplianceEvaluationResponseDTO> cancel(@PathVariable String id) {
        return ResponseEntity.ok(evaluationService.cancel(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        evaluationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Items ────────────────────────────────────────────────────────────────────

    @PostMapping("/{evalId}/items")
    public ResponseEntity<ComplianceEvaluationItemResponseDTO> addItem(
            @PathVariable String evalId,
            @Valid @RequestBody ComplianceEvaluationItemRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(evaluationService.addItem(evalId, request));
    }

    @PatchMapping("/{evalId}/items/{itemId}")
    public ResponseEntity<ComplianceEvaluationItemResponseDTO> updateItem(
            @PathVariable String evalId,
            @PathVariable String itemId,
            @RequestBody ComplianceEvaluationItemRequestDTO request) {
        return ResponseEntity.ok(evaluationService.updateItem(evalId, itemId, request));
    }

    @DeleteMapping("/{evalId}/items/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable String evalId,
            @PathVariable String itemId) {
        evaluationService.deleteItem(evalId, itemId);
        return ResponseEntity.noContent().build();
    }
}