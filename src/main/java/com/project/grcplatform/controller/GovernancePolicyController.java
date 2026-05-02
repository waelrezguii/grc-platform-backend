package com.project.grcplatform.controller;

import com.project.grcplatform.constant.PolicyStatus;
import com.project.grcplatform.dto.GovernancePolicyRequestDTO;
import com.project.grcplatform.dto.GovernancePolicyResponseDTO;
import com.project.grcplatform.model.GovernancePolicyHistory;
import com.project.grcplatform.service.GovernancePolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/governance/policies")
@RequiredArgsConstructor
public class GovernancePolicyController {

    private final GovernancePolicyService policyService;

    @GetMapping
    public ResponseEntity<Page<GovernancePolicyResponseDTO>> findAll(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String policyTypeId,
            @RequestParam(required = false) PolicyStatus status,
            @RequestParam(required = false) String ownerId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(policyService.findAll(title, policyTypeId, status, ownerId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GovernancePolicyResponseDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(policyService.findById(id));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<GovernancePolicyHistory>> getHistory(@PathVariable String id) {
        return ResponseEntity.ok(policyService.getHistory(id));
    }

    @PostMapping
    public ResponseEntity<GovernancePolicyResponseDTO> create(@Valid @RequestBody GovernancePolicyRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(policyService.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<GovernancePolicyResponseDTO> update(
            @PathVariable String id,
            @RequestBody GovernancePolicyRequestDTO request
    ) {
        return ResponseEntity.ok(policyService.update(id, request));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<GovernancePolicyResponseDTO> submit(@PathVariable String id) {
        return ResponseEntity.ok(policyService.submit(id));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<GovernancePolicyResponseDTO> approve(@PathVariable String id) {
        return ResponseEntity.ok(policyService.approve(id));
    }

    @PostMapping("/{id}/deprecate")
    public ResponseEntity<GovernancePolicyResponseDTO> deprecate(@PathVariable String id) {
        return ResponseEntity.ok(policyService.deprecate(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        policyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
