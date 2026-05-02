package com.project.grcplatform.controller;

import com.project.grcplatform.constant.AssessmentStatus;
import com.project.grcplatform.dto.AddScenarioRequestDTO;
import com.project.grcplatform.dto.RiskAssessmentRequestDTO;
import com.project.grcplatform.dto.RiskAssessmentResponseDTO;
import com.project.grcplatform.model.RiskAssessmentHistory;
import com.project.grcplatform.model.RiskAssessmentScenario;
import com.project.grcplatform.service.RiskAssessmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assessments")
@RequiredArgsConstructor
public class RiskAssessmentController {

    private final RiskAssessmentService assessmentService;

    @GetMapping
    public ResponseEntity<Page<RiskAssessmentResponseDTO>> findAll(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String organisationId,
            @RequestParam(required = false) AssessmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return ResponseEntity.ok(
                assessmentService.findAll(title, organisationId, status, PageRequest.of(page, size, sort))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<RiskAssessmentResponseDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(assessmentService.findById(id));
    }

    @PostMapping
    public ResponseEntity<RiskAssessmentResponseDTO> create(@Valid @RequestBody RiskAssessmentRequestDTO request) {
        return ResponseEntity.ok(assessmentService.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<RiskAssessmentResponseDTO> update(
            @PathVariable String id,
            @RequestBody RiskAssessmentRequestDTO request
    ) {
        return ResponseEntity.ok(assessmentService.update(id, request));
    }

    // --- Lifecycle ---

    @PostMapping("/{id}/start")
    public ResponseEntity<RiskAssessmentResponseDTO> start(@PathVariable String id) {
        return ResponseEntity.ok(assessmentService.start(id));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<RiskAssessmentResponseDTO> submitForReview(@PathVariable String id) {
        return ResponseEntity.ok(assessmentService.submitForReview(id));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<RiskAssessmentResponseDTO> complete(@PathVariable String id) {
        return ResponseEntity.ok(assessmentService.complete(id));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<RiskAssessmentResponseDTO> archive(@PathVariable String id) {
        return ResponseEntity.ok(assessmentService.archive(id));
    }

    // --- Scenarios ---

    @GetMapping("/{id}/scenarios")
    public ResponseEntity<List<RiskAssessmentScenario>> getScenarios(@PathVariable String id) {
        return ResponseEntity.ok(assessmentService.getScenarios(id));
    }

    @PostMapping("/{id}/scenarios")
    public ResponseEntity<RiskAssessmentResponseDTO> addScenario(
            @PathVariable String id,
            @Valid @RequestBody AddScenarioRequestDTO request
    ) {
        return ResponseEntity.ok(assessmentService.addScenario(id, request));
    }

    @DeleteMapping("/{id}/scenarios/{scenarioId}")
    public ResponseEntity<RiskAssessmentResponseDTO> removeScenario(
            @PathVariable String id,
            @PathVariable String scenarioId
    ) {
        return ResponseEntity.ok(assessmentService.removeScenario(id, scenarioId));
    }

    // --- Delete & History ---

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        assessmentService.delete(id);
        return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "Assessment deleted successfully"));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<RiskAssessmentHistory>> getHistory(@PathVariable String id) {
        return ResponseEntity.ok(assessmentService.getHistory(id));
    }
}