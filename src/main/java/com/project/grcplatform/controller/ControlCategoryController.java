package com.project.grcplatform.controller;

import com.project.grcplatform.dto.ControlCategoryDTO;
import com.project.grcplatform.service.ControlCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/control-categories")
@RequiredArgsConstructor
public class ControlCategoryController {

    private final ControlCategoryService service;

    @GetMapping
    public ResponseEntity<List<ControlCategoryDTO>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ControlCategoryDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<ControlCategoryDTO> create(@Valid @RequestBody ControlCategoryDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ControlCategoryDTO> update(@PathVariable String id,
                                                     @RequestBody ControlCategoryDTO request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
