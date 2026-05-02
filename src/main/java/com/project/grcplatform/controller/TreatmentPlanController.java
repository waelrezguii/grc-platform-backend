package com.project.grcplatform.controller;

import com.project.grcplatform.constant.TreatmentPlanStatus;
import com.project.grcplatform.constant.TreatmentStrategy;
import com.project.grcplatform.dto.*;
import com.project.grcplatform.model.TreatmentPlanHistory;
import com.project.grcplatform.service.TreatmentPlanService;
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
@RequestMapping("/api/treatment-plans")
@RequiredArgsConstructor
public class TreatmentPlanController {

    private final TreatmentPlanService planService;

    // ---- Plan endpoints ----

    @GetMapping
    public ResponseEntity<Page<TreatmentPlanResponseDTO>> findAll(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String scenarioId,
            @RequestParam(required = false) String assessmentId,
            @RequestParam(required = false) String ownerId,
            @RequestParam(required = false) TreatmentPlanStatus status,
            @RequestParam(required = false) TreatmentStrategy treatmentStrategy,
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
                planService.findAll(title, scenarioId, assessmentId, ownerId, status, treatmentStrategy,
                        archived, PageRequest.of(page, size, sort))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<TreatmentPlanResponseDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(planService.findById(id));
    }

    @PostMapping
    public ResponseEntity<TreatmentPlanResponseDTO> create(@Valid @RequestBody TreatmentPlanRequestDTO request) {
        return ResponseEntity.ok(planService.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TreatmentPlanResponseDTO> update(
            @PathVariable String id,
            @RequestBody TreatmentPlanRequestDTO request
    ) {
        return ResponseEntity.ok(planService.update(id, request));
    }

    // ---- Lifecycle ----

    @PostMapping("/{id}/approve")
    public ResponseEntity<TreatmentPlanResponseDTO> approve(@PathVariable String id) {
        return ResponseEntity.ok(planService.approve(id));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<TreatmentPlanResponseDTO> start(@PathVariable String id) {
        return ResponseEntity.ok(planService.startProgress(id));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<TreatmentPlanResponseDTO> complete(@PathVariable String id) {
        return ResponseEntity.ok(planService.complete(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<TreatmentPlanResponseDTO> cancel(@PathVariable String id) {
        return ResponseEntity.ok(planService.cancel(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        planService.delete(id);
        return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "Treatment plan deleted successfully"));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<TreatmentPlanHistory>> getHistory(@PathVariable String id) {
        return ResponseEntity.ok(planService.getHistory(id));
    }

    // ---- Action endpoints ----

    @GetMapping("/{id}/actions")
    public ResponseEntity<List<TreatmentActionResponseDTO>> getActions(@PathVariable String id) {
        return ResponseEntity.ok(planService.getActions(id));
    }

    @PostMapping("/{id}/actions")
    public ResponseEntity<TreatmentActionResponseDTO> addAction(
            @PathVariable String id,
            @Valid @RequestBody TreatmentActionRequestDTO request
    ) {
        return ResponseEntity.ok(planService.addAction(id, request));
    }

    @PatchMapping("/{id}/actions/{actionId}")
    public ResponseEntity<TreatmentActionResponseDTO> updateAction(
            @PathVariable String id,
            @PathVariable String actionId,
            @RequestBody TreatmentActionRequestDTO request
    ) {
        return ResponseEntity.ok(planService.updateAction(id, actionId, request));
    }

    @PostMapping("/{id}/actions/{actionId}/complete")
    public ResponseEntity<TreatmentActionResponseDTO> completeAction(
            @PathVariable String id,
            @PathVariable String actionId
    ) {
        return ResponseEntity.ok(planService.completeAction(id, actionId));
    }

    @PostMapping("/{id}/actions/{actionId}/cancel")
    public ResponseEntity<TreatmentActionResponseDTO> cancelAction(
            @PathVariable String id,
            @PathVariable String actionId
    ) {
        return ResponseEntity.ok(planService.cancelAction(id, actionId));
    }

    @DeleteMapping("/{id}/actions/{actionId}")
    public ResponseEntity<?> deleteAction(
            @PathVariable String id,
            @PathVariable String actionId
    ) {
        planService.deleteAction(id, actionId);
        return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "Action deleted successfully"));
    }
}