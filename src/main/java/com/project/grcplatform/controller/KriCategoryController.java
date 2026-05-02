package com.project.grcplatform.controller;

import com.project.grcplatform.dto.KriCategoryDTO;
import com.project.grcplatform.service.KriCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kri-categories")
@RequiredArgsConstructor
public class KriCategoryController {

    private final KriCategoryService service;

    @GetMapping
    public ResponseEntity<List<KriCategoryDTO>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<KriCategoryDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<KriCategoryDTO> create(@Valid @RequestBody KriCategoryDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<KriCategoryDTO> update(@PathVariable String id,
                                                  @RequestBody KriCategoryDTO request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
