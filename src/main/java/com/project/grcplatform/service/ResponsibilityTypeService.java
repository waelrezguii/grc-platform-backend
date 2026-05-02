package com.project.grcplatform.service;

import com.project.grcplatform.dto.ResponsibilityTypeDTO;
import com.project.grcplatform.exception.BadRequestException;
import com.project.grcplatform.exception.NotFoundException;
import com.project.grcplatform.model.ResponsibilityType;
import com.project.grcplatform.repository.ResponsibilityTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResponsibilityTypeService {

    private final ResponsibilityTypeRepository repository;

    public List<ResponsibilityTypeDTO> findAll() {
        return repository.findAll().stream().map(this::toDTO).toList();
    }

    public ResponsibilityTypeDTO findById(String id) {
        return toDTO(getOrThrow(id));
    }

    @Transactional
    public ResponsibilityTypeDTO create(ResponsibilityTypeDTO request) {
        if (repository.existsByName(request.getName()))
            throw new BadRequestException("Responsibility type name already exists: " + request.getName());
        ResponsibilityType saved = repository.save(ResponsibilityType.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build());
        return toDTO(saved);
    }

    @Transactional
    public ResponsibilityTypeDTO update(String id, ResponsibilityTypeDTO request) {
        ResponsibilityType entity = getOrThrow(id);
        if (request.getName() != null) entity.setName(request.getName());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        return toDTO(repository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        repository.delete(getOrThrow(id));
    }

    private ResponsibilityType getOrThrow(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Responsibility type not found: " + id));
    }

    private ResponsibilityTypeDTO toDTO(ResponsibilityType e) {
        return ResponsibilityTypeDTO.builder()
                .id(e.getId())
                .name(e.getName())
                .description(e.getDescription())
                .build();
    }
}
