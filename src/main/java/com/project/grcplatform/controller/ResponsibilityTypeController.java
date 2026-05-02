package com.project.grcplatform.controller;

import com.project.grcplatform.dto.ResponsibilityTypeDTO;
import com.project.grcplatform.service.ResponsibilityTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/responsibility-types")
@RequiredArgsConstructor
public class ResponsibilityTypeController {

    private final ResponsibilityTypeService service;

    @GetMapping
    public ResponseEntity<List<ResponsibilityTypeDTO>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponsibilityTypeDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<ResponsibilityTypeDTO> create(@Valid @RequestBody ResponsibilityTypeDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ResponsibilityTypeDTO> update(@PathVariable String id,
                                                        @RequestBody ResponsibilityTypeDTO request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
