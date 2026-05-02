package com.project.grcplatform.service;

import com.project.grcplatform.dto.AuditTypeDTO;
import com.project.grcplatform.exception.BadRequestException;
import com.project.grcplatform.exception.NotFoundException;
import com.project.grcplatform.model.AuditType;
import com.project.grcplatform.repository.AuditTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditTypeService {

    private final AuditTypeRepository repository;

    public List<AuditTypeDTO> findAll() {
        return repository.findAll().stream().map(this::toDTO).toList();
    }

    public AuditTypeDTO findById(String id) {
        return toDTO(getOrThrow(id));
    }

    @Transactional
    public AuditTypeDTO create(AuditTypeDTO request) {
        if (repository.existsByName(request.getName()))
            throw new BadRequestException("Audit type name already exists: " + request.getName());
        AuditType saved = repository.save(AuditType.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build());
        return toDTO(saved);
    }

    @Transactional
    public AuditTypeDTO update(String id, AuditTypeDTO request) {
        AuditType type = getOrThrow(id);
        if (request.getName() != null) type.setName(request.getName());
        if (request.getDescription() != null) type.setDescription(request.getDescription());
        return toDTO(repository.save(type));
    }

    @Transactional
    public void delete(String id) {
        repository.delete(getOrThrow(id));
    }

    private AuditType getOrThrow(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Audit type not found: " + id));
    }

    private AuditTypeDTO toDTO(AuditType t) {
        return AuditTypeDTO.builder()
                .id(t.getId())
                .name(t.getName())
                .description(t.getDescription())
                .build();
    }
}
