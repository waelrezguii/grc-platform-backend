package com.project.grcplatform.service;

import com.project.grcplatform.constant.AuditAction;
import com.project.grcplatform.constant.AuditEntityType;
import com.project.grcplatform.constant.ErrorCode;
import com.project.grcplatform.dto.OrganisationDTO;
import com.project.grcplatform.exception.AppException;
import com.project.grcplatform.mapper.OrganisationMapper;
import com.project.grcplatform.model.Organisation;
import com.project.grcplatform.repository.OrganisationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrganisationService {

    private final OrganisationRepository repository;
    private final AuditService auditService;

    public List<OrganisationDTO> getAll() {
        return repository.findAll().stream().map(OrganisationMapper::toDTO).toList();
    }

    public List<OrganisationDTO> getRoots() {
        return repository.findByParentIsNull().stream().map(OrganisationMapper::toDTO).toList();
    }

    public List<OrganisationDTO> getChildren(String parentId) {
        repository.findById(parentId)
                .orElseThrow(() -> new AppException(ErrorCode.ORGANISATION_NOT_FOUND));
        return repository.findByParent_Id(parentId).stream().map(OrganisationMapper::toDTO).toList();
    }

    public OrganisationDTO create(OrganisationDTO dto) {
        Organisation org = OrganisationMapper.toEntity(dto);

        if (dto.getParentId() != null) {
            Organisation parent = repository.findById(dto.getParentId())
                    .orElseThrow(() -> new AppException(ErrorCode.ORGANISATION_NOT_FOUND));
            org.setParent(parent);
        }

        Organisation saved = repository.save(org);
        auditService.log(AuditAction.USER_CREATED, AuditEntityType.ASSET,
                saved.getId(), "Organisation created: " + saved.getName());
        return OrganisationMapper.toDTO(saved);
    }

    public OrganisationDTO update(String id, OrganisationDTO dto) {
        Organisation org = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ORGANISATION_NOT_FOUND));

        Map<String, Object> oldValues = Map.of(
                "name",  org.getName()  != null ? org.getName()  : "",
                "level", org.getLevel() != null ? org.getLevel() : ""
        );

        if (dto.getCode()        != null) org.setCode(dto.getCode());
        if (dto.getName()        != null) org.setName(dto.getName());
        if (dto.getDescription() != null) org.setDescription(dto.getDescription());
        if (dto.getPhoneNumber() != null) org.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getSortOrder()   != null) org.setSortOrder(dto.getSortOrder());
        if (dto.getLevel()       != null) org.setLevel(dto.getLevel());

        if (dto.getParentId() != null) {
            Organisation parent = repository.findById(dto.getParentId())
                    .orElseThrow(() -> new AppException(ErrorCode.ORGANISATION_NOT_FOUND));
            org.setParent(parent);
        }

        repository.save(org);

        auditService.log(AuditAction.USER_UPDATED, AuditEntityType.ASSET, id,
                "Organisation updated: " + org.getName(),
                oldValues,
                Map.of("name", org.getName() != null ? org.getName() : "",
                       "level", org.getLevel() != null ? org.getLevel() : ""));

        return OrganisationMapper.toDTO(org);
    }

    public void delete(String id) {
        repository.findById(id).ifPresent(org ->
                auditService.log(AuditAction.USER_DELETED, AuditEntityType.ASSET,
                        id, "Organisation deleted: " + org.getName()));
        repository.deleteById(id);
    }
}
