package com.project.grcplatform.controller;

import com.project.grcplatform.dto.ImpactEvaluationRequestDTO;
import com.project.grcplatform.dto.ImpactEvaluationResponseDTO;
import com.project.grcplatform.model.ImpactEvaluationHistory;
import com.project.grcplatform.service.ImpactEvaluationService;
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
@RequestMapping("/api/impact-evaluations")
@RequiredArgsConstructor
public class ImpactEvaluationController {

    private final ImpactEvaluationService evaluationService;

    @GetMapping
    public ResponseEntity<Page<ImpactEvaluationResponseDTO>> findAll(
            @RequestParam(required = false) String vulnerabilityId,
            @RequestParam(required = false) String scenarioId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return ResponseEntity.ok(
                evaluationService.findAll(vulnerabilityId, scenarioId, PageRequest.of(page, size, sort))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImpactEvaluationResponseDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(evaluationService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ImpactEvaluationResponseDTO> create(@Valid @RequestBody ImpactEvaluationRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(evaluationService.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ImpactEvaluationResponseDTO> update(
            @PathVariable String id,
            @RequestBody ImpactEvaluationRequestDTO request
    ) {
        return ResponseEntity.ok(evaluationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        evaluationService.delete(id);
        return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "Impact evaluation deleted successfully"));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<ImpactEvaluationHistory>> getHistory(@PathVariable String id) {
        return ResponseEntity.ok(evaluationService.getHistory(id));
    }
}
