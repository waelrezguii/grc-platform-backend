package com.project.grcplatform.service;

import com.project.grcplatform.dto.ControlCategoryDTO;
import com.project.grcplatform.exception.BadRequestException;
import com.project.grcplatform.exception.NotFoundException;
import com.project.grcplatform.model.ControlCategory;
import com.project.grcplatform.repository.ControlCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ControlCategoryService {

    private final ControlCategoryRepository repository;

    public List<ControlCategoryDTO> findAll() {
        return repository.findAll().stream().map(this::toDTO).toList();
    }

    public ControlCategoryDTO findById(String id) {
        return toDTO(getOrThrow(id));
    }

    @Transactional
    public ControlCategoryDTO create(ControlCategoryDTO request) {
        if (repository.existsByName(request.getName()))
            throw new BadRequestException("Control category name already exists: " + request.getName());
        ControlCategory saved = repository.save(ControlCategory.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build());
        return toDTO(saved);
    }

    @Transactional
    public ControlCategoryDTO update(String id, ControlCategoryDTO request) {
        ControlCategory entity = getOrThrow(id);
        if (request.getName() != null) entity.setName(request.getName());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        return toDTO(repository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        repository.delete(getOrThrow(id));
    }

    private ControlCategory getOrThrow(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Control category not found: " + id));
    }

    private ControlCategoryDTO toDTO(ControlCategory e) {
        return ControlCategoryDTO.builder()
                .id(e.getId())
                .name(e.getName())
                .description(e.getDescription())
                .build();
    }
}
