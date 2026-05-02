package com.project.grcplatform.controller;

import com.project.grcplatform.constant.ResponsibilityStatus;
import com.project.grcplatform.dto.GovernanceResponsibilityRequestDTO;
import com.project.grcplatform.dto.GovernanceResponsibilityResponseDTO;
import com.project.grcplatform.model.GovernanceResponsibilityHistory;
import com.project.grcplatform.service.GovernanceResponsibilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/governance/responsibilities")
@RequiredArgsConstructor
public class GovernanceResponsibilityController {

    private final GovernanceResponsibilityService responsibilityService;

    @GetMapping
    public ResponseEntity<Page<GovernanceResponsibilityResponseDTO>> findAll(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String responsibilityTypeId,
            @RequestParam(required = false) ResponsibilityStatus status,
            @RequestParam(required = false) String assigneeId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                responsibilityService.findAll(title, responsibilityTypeId, status, assigneeId, pageable)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<GovernanceResponsibilityResponseDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(responsibilityService.findById(id));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<GovernanceResponsibilityHistory>> getHistory(@PathVariable String id) {
        return ResponseEntity.ok(responsibilityService.getHistory(id));
    }

    @PostMapping
    public ResponseEntity<GovernanceResponsibilityResponseDTO> create(
            @Valid @RequestBody GovernanceResponsibilityRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(responsibilityService.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<GovernanceResponsibilityResponseDTO> update(
            @PathVariable String id,
            @RequestBody GovernanceResponsibilityRequestDTO request
    ) {
        return ResponseEntity.ok(responsibilityService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<GovernanceResponsibilityResponseDTO> updateStatus(
            @PathVariable String id,
            @RequestParam ResponsibilityStatus status
    ) {
        return ResponseEntity.ok(responsibilityService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        responsibilityService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
