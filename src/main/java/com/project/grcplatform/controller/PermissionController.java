package com.project.grcplatform.controller;

import com.project.grcplatform.model.Permission;
import com.project.grcplatform.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    public List<Permission> getAll() {
        return permissionService.getAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Permission create(@RequestBody Permission permission) {
        return permissionService.create(permission);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Permission update(@PathVariable Long id, @RequestBody Permission permission) {
        return permissionService.update(id, permission);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        permissionService.delete(id);
    }
}