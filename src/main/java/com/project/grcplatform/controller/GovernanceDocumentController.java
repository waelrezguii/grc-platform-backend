package com.project.grcplatform.controller;
import com.project.grcplatform.constant.DocumentStatus;
import com.project.grcplatform.dto.GovernanceDocumentRequestDTO;
import com.project.grcplatform.dto.GovernanceDocumentResponseDTO;
import com.project.grcplatform.model.GovernanceDocumentHistory;
import com.project.grcplatform.service.GovernanceDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/governance/documents")
@RequiredArgsConstructor
public class GovernanceDocumentController {

    private final GovernanceDocumentService documentService;

    @GetMapping
    public ResponseEntity<Page<GovernanceDocumentResponseDTO>> findAll(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String documentTypeId,
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) String linkedPolicyId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(documentService.findAll(title, documentTypeId, status, linkedPolicyId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GovernanceDocumentResponseDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(documentService.findById(id));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<GovernanceDocumentHistory>> getHistory(@PathVariable String id) {
        return ResponseEntity.ok(documentService.getHistory(id));
    }

    @PostMapping
    public ResponseEntity<GovernanceDocumentResponseDTO> create(
            @Valid @RequestBody GovernanceDocumentRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<GovernanceDocumentResponseDTO> update(
            @PathVariable String id,
            @RequestBody GovernanceDocumentRequestDTO request
    ) {
        return ResponseEntity.ok(documentService.update(id, request));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<GovernanceDocumentResponseDTO> publish(@PathVariable String id) {
        return ResponseEntity.ok(documentService.publish(id));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<GovernanceDocumentResponseDTO> archive(@PathVariable String id) {
        return ResponseEntity.ok(documentService.archive(id));
    }

    @PostMapping("/{id}/obsolete")
    public ResponseEntity<GovernanceDocumentResponseDTO> markObsolete(@PathVariable String id) {
        return ResponseEntity.ok(documentService.markObsolete(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }
    @PostMapping(value = "/{id}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GovernanceDocumentResponseDTO> uploadFile(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        String uploadDir = "uploads/governance/";
        Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(base);
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String safeName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String filename = id + "_" + safeName;
        Path path = base.resolve(filename).normalize();
        if (!path.startsWith(base)) {
            throw new IOException("Invalid file path");
        }
        Files.write(path, file.getBytes());
        String fileUrl = "/uploads/governance/" + filename;

        GovernanceDocumentRequestDTO dto = new GovernanceDocumentRequestDTO();
        dto.setFileUrl(fileUrl);
        dto.setFileSize(file.getSize());
        dto.setMimeType(file.getContentType());

        return ResponseEntity.ok(documentService.update(id, dto));
    }
}
