package com.project.grcplatform.service;

import com.project.grcplatform.constant.AuditAction;
import com.project.grcplatform.constant.AuditEntityType;
import com.project.grcplatform.constant.ErrorCode;
import com.project.grcplatform.exception.AppException;
import com.project.grcplatform.model.Permission;
import com.project.grcplatform.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final AuditService auditService;

    public List<Permission> getAll() {
        return permissionRepository.findAll();
    }

    public Permission create(Permission permission) {
        Permission saved = permissionRepository.save(permission);

        auditService.log(
                AuditAction.USER_CREATED,
                AuditEntityType.USER,
                String.valueOf(saved.getId()),
                "Permission created: " + saved.getName()
        );

        return saved;
    }

    public Permission update(Long id, Permission permission) {
        Permission existing = permissionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_FOUND));

        Map<String, Object> oldValues = Map.of(
                "name", existing.getName() != null ? existing.getName() : ""
        );

        if (permission.getName() != null) existing.setName(permission.getName());

        Permission saved = permissionRepository.save(existing);

        Map<String, Object> newValues = Map.of(
                "name", saved.getName() != null ? saved.getName() : ""
        );

        auditService.log(
                AuditAction.USER_UPDATED,
                AuditEntityType.USER,
                String.valueOf(id),
                "Permission updated: " + saved.getName(),
                oldValues,
                newValues
        );

        return saved;
    }

    public void delete(Long id) {
        permissionRepository.findById(id).ifPresent(p ->
                auditService.log(
                        AuditAction.USER_DELETED,
                        AuditEntityType.USER,
                        String.valueOf(id),
                        "Permission deleted: " + p.getName()
                )
        );
        permissionRepository.deleteById(id);
    }
}