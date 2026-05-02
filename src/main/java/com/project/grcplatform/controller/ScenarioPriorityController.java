package com.project.grcplatform.controller;

import com.project.grcplatform.dto.ScenarioPriorityConfigRequestDTO;
import com.project.grcplatform.dto.ScenarioPriorityConfigResponseDTO;
import com.project.grcplatform.model.ScenarioPriorityConfigHistory;
import com.project.grcplatform.service.ScenarioPriorityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/priority-config")
@RequiredArgsConstructor
public class ScenarioPriorityController {

    private final ScenarioPriorityService priorityService;

    @GetMapping
    public ResponseEntity<Page<ScenarioPriorityConfigResponseDTO>> findAll(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return ResponseEntity.ok(priorityService.findAll(name, PageRequest.of(page, size, sort)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScenarioPriorityConfigResponseDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(priorityService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ScenarioPriorityConfigResponseDTO> create(
            @Valid @RequestBody ScenarioPriorityConfigRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(priorityService.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ScenarioPriorityConfigResponseDTO> update(
            @PathVariable String id,
            @RequestBody ScenarioPriorityConfigRequestDTO request) {
        return ResponseEntity.ok(priorityService.update(id, request));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ScenarioPriorityConfigResponseDTO> activate(@PathVariable String id) {
        return ResponseEntity.ok(priorityService.activate(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        priorityService.delete(id);
        return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "Priority config deleted successfully"));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<ScenarioPriorityConfigHistory>> getHistory(@PathVariable String id) {
        return ResponseEntity.ok(priorityService.getHistory(id));
    }
}
