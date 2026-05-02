package com.project.grcplatform.service;

import com.project.grcplatform.dto.PolicyTypeDTO;
import com.project.grcplatform.exception.BadRequestException;
import com.project.grcplatform.exception.NotFoundException;
import com.project.grcplatform.model.PolicyType;
import com.project.grcplatform.repository.PolicyTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyTypeService {

    private final PolicyTypeRepository repository;

    public List<PolicyTypeDTO> findAll() {
        return repository.findAll().stream().map(this::toDTO).toList();
    }

    public PolicyTypeDTO findById(String id) {
        return toDTO(getOrThrow(id));
    }

    @Transactional
    public PolicyTypeDTO create(PolicyTypeDTO request) {
        if (repository.existsByName(request.getName()))
            throw new BadRequestException("Policy type name already exists: " + request.getName());
        PolicyType saved = repository.save(PolicyType.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build());
        return toDTO(saved);
    }

    @Transactional
    public PolicyTypeDTO update(String id, PolicyTypeDTO request) {
        PolicyType entity = getOrThrow(id);
        if (request.getName() != null) entity.setName(request.getName());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        return toDTO(repository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        repository.delete(getOrThrow(id));
    }

    private PolicyType getOrThrow(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Policy type not found: " + id));
    }

    private PolicyTypeDTO toDTO(PolicyType e) {
        return PolicyTypeDTO.builder()
                .id(e.getId())
                .name(e.getName())
                .description(e.getDescription())
                .build();
    }
}
