package com.project.grcplatform.controller;

import com.project.grcplatform.constant.ActionPlanPriority;
import com.project.grcplatform.constant.ActionPlanStatus;
import com.project.grcplatform.dto.*;
import com.project.grcplatform.model.ComplianceActionPlanHistory;
import com.project.grcplatform.service.ComplianceActionPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/compliance/action-plans")
@RequiredArgsConstructor
public class ComplianceActionPlanController {

    private final ComplianceActionPlanService actionPlanService;

    @GetMapping
    public ResponseEntity<Page<ComplianceActionPlanResponseDTO>> findAll(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String evaluationId,
            @RequestParam(required = false) ActionPlanStatus status,
            @RequestParam(required = false) String assigneeId,
            @RequestParam(required = false) ActionPlanPriority priority,
            Pageable pageable) {
        return ResponseEntity.ok(
                actionPlanService.findAll(title, evaluationId, status, assigneeId, priority, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComplianceActionPlanResponseDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(actionPlanService.findById(id));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<ComplianceActionPlanHistory>> getHistory(@PathVariable String id) {
        return ResponseEntity.ok(actionPlanService.getHistory(id));
    }

    @PostMapping
    public ResponseEntity<ComplianceActionPlanResponseDTO> create(
            @Valid @RequestBody ComplianceActionPlanRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(actionPlanService.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ComplianceActionPlanResponseDTO> update(
            @PathVariable String id, @RequestBody ComplianceActionPlanRequestDTO request) {
        return ResponseEntity.ok(actionPlanService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ComplianceActionPlanResponseDTO> updateStatus(
            @PathVariable String id, @RequestParam ActionPlanStatus status) {
        return ResponseEntity.ok(actionPlanService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        actionPlanService.delete(id);
        return ResponseEntity.noContent().build();
    }
}