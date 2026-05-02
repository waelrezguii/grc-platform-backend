package com.project.grcplatform.controller;

import com.project.grcplatform.constant.FrameworkStatus;
import com.project.grcplatform.constant.FrameworkType;
import com.project.grcplatform.dto.*;
import com.project.grcplatform.model.ComplianceFrameworkHistory;
import com.project.grcplatform.service.ComplianceFrameworkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/compliance/frameworks")
@RequiredArgsConstructor
public class ComplianceFrameworkController {

    private final ComplianceFrameworkService frameworkService;

    @GetMapping
    public ResponseEntity<Page<ComplianceFrameworkResponseDTO>> findAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) FrameworkType frameworkType,
            @RequestParam(required = false) FrameworkStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(frameworkService.findAll(name, frameworkType, status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComplianceFrameworkResponseDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(frameworkService.findById(id));
    }

    @GetMapping("/{id}/requirements")
    public ResponseEntity<List<ComplianceRequirementResponseDTO>> getRequirements(@PathVariable String id) {
        return ResponseEntity.ok(frameworkService.getRequirements(id));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<ComplianceFrameworkHistory>> getHistory(@PathVariable String id) {
        return ResponseEntity.ok(frameworkService.getHistory(id));
    }

    @PostMapping
    public ResponseEntity<ComplianceFrameworkResponseDTO> create(
            @Valid @RequestBody ComplianceFrameworkRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(frameworkService.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ComplianceFrameworkResponseDTO> update(
            @PathVariable String id, @RequestBody ComplianceFrameworkRequestDTO request) {
        return ResponseEntity.ok(frameworkService.update(id, request));
    }

    @PostMapping("/{id}/deprecate")
    public ResponseEntity<ComplianceFrameworkResponseDTO> deprecate(@PathVariable String id) {
        return ResponseEntity.ok(frameworkService.deprecate(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        frameworkService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Requirements (nested under framework) ───────────────────────────────────

    @PostMapping("/requirements")
    public ResponseEntity<ComplianceRequirementResponseDTO> createRequirement(
            @Valid @RequestBody ComplianceRequirementRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(frameworkService.createRequirement(request));
    }

    @PatchMapping("/requirements/{id}")
    public ResponseEntity<ComplianceRequirementResponseDTO> updateRequirement(
            @PathVariable String id, @RequestBody ComplianceRequirementRequestDTO request) {
        return ResponseEntity.ok(frameworkService.updateRequirement(id, request));
    }

    @DeleteMapping("/requirements/{id}")
    public ResponseEntity<Void> deleteRequirement(@PathVariable String id) {
        frameworkService.deleteRequirement(id);
        return ResponseEntity.noContent().build();
    }
}