package com.project.grcplatform.controller;

import com.project.grcplatform.constant.ReportStatus;
import com.project.grcplatform.constant.ReportType;
import com.project.grcplatform.dto.ReportRequestDTO;
import com.project.grcplatform.dto.ReportResponseDTO;
import com.project.grcplatform.service.RiskReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class RiskReportController {

    private final RiskReportService reportService;

    @GetMapping
    public ResponseEntity<Page<ReportResponseDTO>> findAll(
            @RequestParam(required = false) ReportType type,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) String assessmentId,
            @RequestParam(required = false) String organisationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return ResponseEntity.ok(
                reportService.findAll(type, status, assessmentId, organisationId,
                        PageRequest.of(page, size, sort))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReportResponseDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(reportService.findById(id));
    }

    @PostMapping("/generate")
    public ResponseEntity<ReportResponseDTO> generate(@Valid @RequestBody ReportRequestDTO request) {
        return ResponseEntity.ok(reportService.generate(request));
    }

    @PostMapping("/{id}/regenerate")
    public ResponseEntity<ReportResponseDTO> regenerate(@PathVariable String id) {
        return ResponseEntity.ok(reportService.regenerate(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        reportService.delete(id);
        return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "Report deleted successfully"));
    }
}