package com.project.grcplatform.controller;

import com.project.grcplatform.dto.ArchiveRuleRequestDTO;
import com.project.grcplatform.dto.ArchiveRuleResponseDTO;
import com.project.grcplatform.dto.UnarchiveRequestDTO;
import com.project.grcplatform.service.ArchiveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/archive-rules")
@RequiredArgsConstructor
public class ArchiveRuleController {

    private final ArchiveService archiveService;

    @GetMapping
    public ResponseEntity<List<ArchiveRuleResponseDTO>> findAll() {
        return ResponseEntity.ok(archiveService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArchiveRuleResponseDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(archiveService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ArchiveRuleResponseDTO> create(
            @Valid @RequestBody ArchiveRuleRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(archiveService.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ArchiveRuleResponseDTO> update(
            @PathVariable String id,
            @RequestBody ArchiveRuleRequestDTO request) {
        return ResponseEntity.ok(archiveService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        archiveService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Manually triggers a single archive rule immediately. */
    @PostMapping("/{id}/run")
    public ResponseEntity<ArchiveRuleResponseDTO> runNow(@PathVariable String id) {
        return ResponseEntity.ok(archiveService.runOne(id));
    }

    /** Restores an archived record back to active. */
    @PostMapping("/unarchive")
    public ResponseEntity<Void> unarchive(@Valid @RequestBody UnarchiveRequestDTO request) {
        archiveService.unarchive(request);
        return ResponseEntity.noContent().build();
    }
}
