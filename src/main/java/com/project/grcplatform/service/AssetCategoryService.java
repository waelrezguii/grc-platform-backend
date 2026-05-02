package com.project.grcplatform.service;

import com.project.grcplatform.dto.AssetCategoryDTO;
import com.project.grcplatform.exception.BadRequestException;
import com.project.grcplatform.exception.NotFoundException;
import com.project.grcplatform.model.AssetCategory;
import com.project.grcplatform.repository.AssetCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssetCategoryService {

    private final AssetCategoryRepository repository;

    public List<AssetCategoryDTO> findAll() {
        return repository.findAll().stream().map(this::toDTO).toList();
    }

    public AssetCategoryDTO findById(String id) {
        return toDTO(getOrThrow(id));
    }

    @Transactional
    public AssetCategoryDTO create(AssetCategoryDTO request) {
        if (repository.existsByName(request.getName()))
            throw new BadRequestException("Asset category name already exists: " + request.getName());
        AssetCategory saved = repository.save(AssetCategory.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build());
        return toDTO(saved);
    }

    @Transactional
    public AssetCategoryDTO update(String id, AssetCategoryDTO request) {
        AssetCategory entity = getOrThrow(id);
        if (request.getName() != null) entity.setName(request.getName());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        return toDTO(repository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        repository.delete(getOrThrow(id));
    }

    private AssetCategory getOrThrow(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Asset category not found: " + id));
    }

    private AssetCategoryDTO toDTO(AssetCategory e) {
        return AssetCategoryDTO.builder()
                .id(e.getId())
                .name(e.getName())
                .description(e.getDescription())
                .build();
    }
}
