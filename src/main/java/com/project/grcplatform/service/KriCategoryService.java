package com.project.grcplatform.service;

import com.project.grcplatform.dto.KriCategoryDTO;
import com.project.grcplatform.exception.BadRequestException;
import com.project.grcplatform.exception.NotFoundException;
import com.project.grcplatform.model.KriCategory;
import com.project.grcplatform.repository.KriCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KriCategoryService {

    private final KriCategoryRepository repository;

    public List<KriCategoryDTO> findAll() {
        return repository.findAll().stream().map(this::toDTO).toList();
    }

    public KriCategoryDTO findById(String id) {
        return toDTO(getOrThrow(id));
    }

    @Transactional
    public KriCategoryDTO create(KriCategoryDTO request) {
        if (repository.existsByName(request.getName()))
            throw new BadRequestException("KRI category name already exists: " + request.getName());
        KriCategory saved = repository.save(KriCategory.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build());
        return toDTO(saved);
    }

    @Transactional
    public KriCategoryDTO update(String id, KriCategoryDTO request) {
        KriCategory cat = getOrThrow(id);
        if (request.getName() != null) cat.setName(request.getName());
        if (request.getDescription() != null) cat.setDescription(request.getDescription());
        return toDTO(repository.save(cat));
    }

    @Transactional
    public void delete(String id) {
        repository.delete(getOrThrow(id));
    }

    private KriCategory getOrThrow(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("KRI category not found: " + id));
    }

    private KriCategoryDTO toDTO(KriCategory c) {
        return KriCategoryDTO.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .build();
    }
}
