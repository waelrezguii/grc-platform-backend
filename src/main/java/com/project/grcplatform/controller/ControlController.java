package com.project.grcplatform.controller;

import com.project.grcplatform.constant.*;
import com.project.grcplatform.dto.ControlRequestDTO;
import com.project.grcplatform.dto.ControlResponseDTO;
import com.project.grcplatform.dto.ReviewRequestDTO;
import com.project.grcplatform.model.ControlHistory;
import com.project.grcplatform.service.ControlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/controls")
@RequiredArgsConstructor
public class ControlController {

    private final ControlService controlService;

    @GetMapping
    public ResponseEntity<Page<ControlResponseDTO>> findAll(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String isoReference,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String typeId,
            @RequestParam(required = false) ControlStatus status,
            @RequestParam(required = false) ControlEffectiveness effectiveness,
            @RequestParam(required = false) String assetId,
            @RequestParam(required = false) String scenarioId,
            @RequestParam(required = false) String ownerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return ResponseEntity.ok(
                controlService.findAll(title, isoReference, categoryId, typeId, status, effectiveness,
                        assetId, scenarioId, ownerId, PageRequest.of(page, size, sort))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ControlResponseDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(controlService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ControlResponseDTO> create(@Valid @RequestBody ControlRequestDTO request) {
        return ResponseEntity.ok(controlService.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ControlResponseDTO> update(
            @PathVariable String id,
            @RequestBody ControlRequestDTO request
    ) {
        return ResponseEntity.ok(controlService.update(id, request));
    }

    // --- Lifecycle ---

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String typeId,
            @RequestParam(required = false) ControlStatus status,
            @RequestParam(required = false) ControlEffectiveness effectiveness,
            @RequestParam(required = false) String assetId,
            @RequestParam(required = false) String scenarioId
    ) {
        List<ControlResponseDTO> controls = controlService.export(categoryId, typeId, status, effectiveness, assetId, scenarioId);
        String csv = buildCsv(controls);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"controls_export.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    @PostMapping("/{id}/implement")
    public ResponseEntity<ControlResponseDTO> implement(@PathVariable String id) {
        return ResponseEntity.ok(controlService.implement(id));
    }

    @PostMapping("/{id}/validate")
    public ResponseEntity<ControlResponseDTO> validate(@PathVariable String id) {
        return ResponseEntity.ok(controlService.validate(id));
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<ControlResponseDTO> review(
            @PathVariable String id,
            @Valid @RequestBody ReviewRequestDTO request
    ) {
        return ResponseEntity.ok(controlService.review(id, request));
    }

    @PostMapping("/{id}/retire")
    public ResponseEntity<ControlResponseDTO> retire(@PathVariable String id) {
        return ResponseEntity.ok(controlService.retire(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        controlService.delete(id);
        return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "Control deleted successfully"));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<ControlHistory>> getHistory(@PathVariable String id) {
        return ResponseEntity.ok(controlService.getHistory(id));
    }

    // ── CSV builder ───────────────────────────────────────────────────────────

    private String buildCsv(List<ControlResponseDTO> controls) {
        StringBuilder sb = new StringBuilder();
        sb.append("id,isoReference,title,status,effectiveness,scope,category,type,owner,assetId," +
                  "scenarios,validatedBy,validatedAt,lastReviewDate,nextReviewDate,createdAt\n");
        for (ControlResponseDTO c : controls) {
            sb.append(csv(c.getId())).append(',')
              .append(csv(c.getIsoReference())).append(',')
              .append(csv(c.getTitle())).append(',')
              .append(csv(c.getStatus() != null ? c.getStatus().name() : "")).append(',')
              .append(csv(c.getEffectiveness() != null ? c.getEffectiveness().name() : "")).append(',')
              .append(csv(c.getScope())).append(',')
              .append(csv(c.getCategory() != null ? c.getCategory().getName() : "")).append(',')
              .append(csv(c.getType() != null ? c.getType().getName() : "")).append(',')
              .append(csv(c.getOwner() != null ? c.getOwner().getFirstname() + " " + c.getOwner().getLastname() : "")).append(',')
              .append(csv(c.getAssetId())).append(',')
              .append(csv(c.getScenarios() != null
                      ? c.getScenarios().stream().map(s -> s.getName()).reduce("", (a, b) -> a.isEmpty() ? b : a + " | ")
                      : "")).append(',')
              .append(csv(c.getValidatedBy())).append(',')
              .append(csv(c.getValidatedAt() != null ? c.getValidatedAt().toString() : "")).append(',')
              .append(csv(c.getLastReviewDate() != null ? c.getLastReviewDate().toString() : "")).append(',')
              .append(csv(c.getNextReviewDate() != null ? c.getNextReviewDate().toString() : "")).append(',')
              .append(csv(c.getCreatedAt() != null ? c.getCreatedAt().toString() : "")).append('\n');
        }
        return sb.toString();
    }

    private String csv(String value) {
        if (value == null || value.isEmpty()) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
