package com.project.grcplatform.service;

import com.project.grcplatform.dto.AssetTypeDTO;
import com.project.grcplatform.exception.BadRequestException;
import com.project.grcplatform.exception.NotFoundException;
import com.project.grcplatform.model.AssetType;
import com.project.grcplatform.repository.AssetTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssetTypeService {

    private final AssetTypeRepository repository;

    public List<AssetTypeDTO> findAll() {
        return repository.findAll().stream().map(this::toDTO).toList();
    }

    public AssetTypeDTO findById(String id) {
        return toDTO(getOrThrow(id));
    }

    @Transactional
    public AssetTypeDTO create(AssetTypeDTO request) {
        if (repository.existsByName(request.getName()))
            throw new BadRequestException("Asset type name already exists: " + request.getName());
        AssetType saved = repository.save(AssetType.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build());
        return toDTO(saved);
    }

    @Transactional
    public AssetTypeDTO update(String id, AssetTypeDTO request) {
        AssetType entity = getOrThrow(id);
        if (request.getName() != null) entity.setName(request.getName());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        return toDTO(repository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        repository.delete(getOrThrow(id));
    }

    private AssetType getOrThrow(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Asset type not found: " + id));
    }

    private AssetTypeDTO toDTO(AssetType e) {
        return AssetTypeDTO.builder()
                .id(e.getId())
                .name(e.getName())
                .description(e.getDescription())
                .build();
    }
}
