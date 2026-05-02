package com.project.grcplatform.controller;

import com.project.grcplatform.constant.AuditAction;
import com.project.grcplatform.constant.AuditEntityType;
import com.project.grcplatform.dto.AuditLogResponseDTO;
import com.project.grcplatform.mapper.AuditLogMapper;
import com.project.grcplatform.repository.AuditLogRepository;
import com.project.grcplatform.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<Page<AuditLogResponseDTO>> findAll(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) AuditEntityType entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Page<AuditLogResponseDTO> result = auditLogRepository
                .findAllWithFilters(userId, entityType, entityId, action, success, from, to,
                        PageRequest.of(page, size, sort))
                .map(AuditLogMapper::toDTO);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditLogResponseDTO> findById(@PathVariable String id) {
        return auditLogRepository.findById(id)
                .map(AuditLogMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<List<AuditLogResponseDTO>> findByEntity(
            @PathVariable AuditEntityType entityType,
            @PathVariable String entityId
    ) {
        List<AuditLogResponseDTO> result = auditLogRepository
                .findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId)
                .stream()
                .map(AuditLogMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AuditLogResponseDTO>> findByUser(@PathVariable String userId) {
        List<AuditLogResponseDTO> result = auditLogRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(AuditLogMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * Verifies the integrity of the audit chain.
     * Returns { "intact": true } if no tampering is detected,
     * or { "intact": false, "firstTamperedId": "<id>" } if a broken link is found.
     */
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyChain() {
        String tamperedId = auditService.verifyChain();
        if (tamperedId == null) {
            return ResponseEntity.ok(Map.of("intact", true));
        }
        return ResponseEntity.ok(Map.of("intact", false, "firstTamperedId", tamperedId));
    }
}