package com.project.grcplatform.controller;

import com.project.grcplatform.dto.PolicyTypeDTO;
import com.project.grcplatform.service.PolicyTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policy-types")
@RequiredArgsConstructor
public class PolicyTypeController {

    private final PolicyTypeService service;

    @GetMapping
    public ResponseEntity<List<PolicyTypeDTO>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PolicyTypeDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<PolicyTypeDTO> create(@Valid @RequestBody PolicyTypeDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PolicyTypeDTO> update(@PathVariable String id,
                                                @RequestBody PolicyTypeDTO request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
