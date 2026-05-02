package com.project.grcplatform.service;

import com.project.grcplatform.dto.DocumentTypeDTO;
import com.project.grcplatform.exception.BadRequestException;
import com.project.grcplatform.exception.NotFoundException;
import com.project.grcplatform.model.DocumentType;
import com.project.grcplatform.repository.DocumentTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentTypeService {

    private final DocumentTypeRepository repository;

    public List<DocumentTypeDTO> findAll() {
        return repository.findAll().stream().map(this::toDTO).toList();
    }

    public DocumentTypeDTO findById(String id) {
        return toDTO(getOrThrow(id));
    }

    @Transactional
    public DocumentTypeDTO create(DocumentTypeDTO request) {
        if (repository.existsByName(request.getName()))
            throw new BadRequestException("Document type name already exists: " + request.getName());
        DocumentType saved = repository.save(DocumentType.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build());
        return toDTO(saved);
    }

    @Transactional
    public DocumentTypeDTO update(String id, DocumentTypeDTO request) {
        DocumentType entity = getOrThrow(id);
        if (request.getName() != null) entity.setName(request.getName());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        return toDTO(repository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        repository.delete(getOrThrow(id));
    }

    private DocumentType getOrThrow(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Document type not found: " + id));
    }

    private DocumentTypeDTO toDTO(DocumentType e) {
        return DocumentTypeDTO.builder()
                .id(e.getId())
                .name(e.getName())
                .description(e.getDescription())
                .build();
    }
}
