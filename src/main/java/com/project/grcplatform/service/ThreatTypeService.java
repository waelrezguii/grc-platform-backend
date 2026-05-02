package com.project.grcplatform.service;

import com.project.grcplatform.dto.ThreatTypeDTO;
import com.project.grcplatform.exception.BadRequestException;
import com.project.grcplatform.exception.NotFoundException;
import com.project.grcplatform.model.ThreatType;
import com.project.grcplatform.repository.ThreatTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ThreatTypeService {

    private final ThreatTypeRepository repository;

    public List<ThreatTypeDTO> findAll() {
        return repository.findAll().stream().map(this::toDTO).toList();
    }

    public ThreatTypeDTO findById(String id) {
        return toDTO(getOrThrow(id));
    }

    @Transactional
    public ThreatTypeDTO create(ThreatTypeDTO request) {
        if (repository.existsByName(request.getName()))
            throw new BadRequestException("Threat type name already exists: " + request.getName());
        ThreatType saved = repository.save(ThreatType.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build());
        return toDTO(saved);
    }

    @Transactional
    public ThreatTypeDTO update(String id, ThreatTypeDTO request) {
        ThreatType entity = getOrThrow(id);
        if (request.getName() != null) entity.setName(request.getName());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        return toDTO(repository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        repository.delete(getOrThrow(id));
    }

    private ThreatType getOrThrow(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Threat type not found: " + id));
    }

    private ThreatTypeDTO toDTO(ThreatType e) {
        return ThreatTypeDTO.builder()
                .id(e.getId())
                .name(e.getName())
                .description(e.getDescription())
                .build();
    }
}
