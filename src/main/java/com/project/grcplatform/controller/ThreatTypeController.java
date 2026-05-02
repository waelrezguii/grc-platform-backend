package com.project.grcplatform.controller;

import com.project.grcplatform.dto.ThreatTypeDTO;
import com.project.grcplatform.service.ThreatTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/threat-types")
@RequiredArgsConstructor
public class ThreatTypeController {

    private final ThreatTypeService service;

    @GetMapping
    public ResponseEntity<List<ThreatTypeDTO>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ThreatTypeDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<ThreatTypeDTO> create(@Valid @RequestBody ThreatTypeDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ThreatTypeDTO> update(@PathVariable String id,
                                                @RequestBody ThreatTypeDTO request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
