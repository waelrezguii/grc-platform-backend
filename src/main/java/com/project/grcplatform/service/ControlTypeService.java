package com.project.grcplatform.service;

import com.project.grcplatform.dto.ControlTypeDTO;
import com.project.grcplatform.exception.BadRequestException;
import com.project.grcplatform.exception.NotFoundException;
import com.project.grcplatform.model.ControlType;
import com.project.grcplatform.repository.ControlTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ControlTypeService {

    private final ControlTypeRepository repository;

    public List<ControlTypeDTO> findAll() {
        return repository.findAll().stream().map(this::toDTO).toList();
    }

    public ControlTypeDTO findById(String id) {
        return toDTO(getOrThrow(id));
    }

    @Transactional
    public ControlTypeDTO create(ControlTypeDTO request) {
        if (repository.existsByName(request.getName()))
            throw new BadRequestException("Control type name already exists: " + request.getName());
        ControlType saved = repository.save(ControlType.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build());
        return toDTO(saved);
    }

    @Transactional
    public ControlTypeDTO update(String id, ControlTypeDTO request) {
        ControlType entity = getOrThrow(id);
        if (request.getName() != null) entity.setName(request.getName());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        return toDTO(repository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        repository.delete(getOrThrow(id));
    }

    private ControlType getOrThrow(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Control type not found: " + id));
    }

    private ControlTypeDTO toDTO(ControlType e) {
        return ControlTypeDTO.builder()
                .id(e.getId())
                .name(e.getName())
                .description(e.getDescription())
                .build();
    }
}
