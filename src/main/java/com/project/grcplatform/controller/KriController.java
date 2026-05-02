package com.project.grcplatform.controller;

import com.project.grcplatform.constant.KriLinkedEntityType;
import com.project.grcplatform.constant.KriStatus;
import com.project.grcplatform.dto.KriRequestDTO;
import com.project.grcplatform.dto.KriResponseDTO;
import com.project.grcplatform.model.KriHistory;
import com.project.grcplatform.service.KriService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kris")
@RequiredArgsConstructor
public class KriController {

    private final KriService kriService;

    @GetMapping
    public ResponseEntity<Page<KriResponseDTO>> findAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) KriStatus status,
            @RequestParam(required = false) KriLinkedEntityType linkedEntityType,
            @RequestParam(required = false) String linkedEntityId,
            @RequestParam(required = false) String ownerId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                kriService.findAll(name, categoryId, status, linkedEntityType, linkedEntityId, ownerId, pageable)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<KriResponseDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(kriService.findById(id));
    }

    @GetMapping("/breached")
    public ResponseEntity<List<KriResponseDTO>> getBreached() {
        return ResponseEntity.ok(kriService.getBreached());
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Long>> getSummary() {
        return ResponseEntity.ok(kriService.getSummary());
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<KriHistory>> getHistory(@PathVariable String id) {
        return ResponseEntity.ok(kriService.getHistory(id));
    }

    @PostMapping
    public ResponseEntity<KriResponseDTO> create(@Valid @RequestBody KriRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(kriService.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<KriResponseDTO> update(
            @PathVariable String id,
            @RequestBody KriRequestDTO request
    ) {
        return ResponseEntity.ok(kriService.update(id, request));
    }

    @PatchMapping("/{id}/value")
    public ResponseEntity<KriResponseDTO> updateValue(
            @PathVariable String id,
            @RequestParam Double value
    ) {
        return ResponseEntity.ok(kriService.updateValue(id, value));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        kriService.delete(id);
        return ResponseEntity.noContent().build();
    }
}