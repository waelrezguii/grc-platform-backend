package com.project.grcplatform.service;

import com.project.grcplatform.constant.AuditAction;
import com.project.grcplatform.constant.AuditEntityType;
import com.project.grcplatform.dto.RoleDTO;
import com.project.grcplatform.mapper.RoleMapper;
import com.project.grcplatform.model.Permission;
import com.project.grcplatform.model.Role;
import com.project.grcplatform.repository.PermissionRepository;
import com.project.grcplatform.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AuditService auditService;

    public List<RoleDTO> getAll() {
        return roleRepository.findAll()
                .stream()
                .map(RoleMapper::toDTO)
                .toList();
    }

    public RoleDTO create(RoleDTO dto) {
        Set<Permission> permissions = resolvePermissions(dto);
        Role role = RoleMapper.toEntity(dto, permissions);
        Role saved = roleRepository.save(role);

        auditService.log(
                AuditAction.USER_CREATED,
                AuditEntityType.USER,
                String.valueOf(saved.getId()),
                "Role created: " + saved.getName()
        );

        return RoleMapper.toDTO(saved);
    }

    public RoleDTO update(Long id, RoleDTO dto) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        Map<String, Object> oldValues = Map.of(
                "name", role.getName() != null ? role.getName() : "",
                "permissions", role.getPermissions().stream()
                        .map(Permission::getName).collect(Collectors.joining(", "))
        );

        Set<Permission> permissions = resolvePermissions(dto);
        RoleMapper.updateEntity(role, dto, permissions);
        Role updated = roleRepository.save(role);

        Map<String, Object> newValues = Map.of(
                "name", updated.getName() != null ? updated.getName() : "",
                "permissions", updated.getPermissions().stream()
                        .map(Permission::getName).collect(Collectors.joining(", "))
        );

        auditService.log(
                AuditAction.USER_UPDATED,
                AuditEntityType.USER,
                String.valueOf(id),
                "Role updated: " + updated.getName(),
                oldValues,
                newValues
        );

        return RoleMapper.toDTO(updated);
    }

    public void delete(Long id) {
        roleRepository.findById(id).ifPresent(role ->
                auditService.log(
                        AuditAction.USER_DELETED,
                        AuditEntityType.USER,
                        String.valueOf(id),
                        "Role deleted: " + role.getName()
                )
        );
        roleRepository.deleteById(id);
    }

    // -------------------------

    private Set<Permission> resolvePermissions(RoleDTO dto) {
        if (dto.getPermissionNames() == null) return Set.of();
        return dto.getPermissionNames().stream()
                .map(name -> permissionRepository.findByName(name)
                        .orElseThrow(() -> new RuntimeException("Permission not found: " + name)))
                .collect(Collectors.toSet());
    }
}