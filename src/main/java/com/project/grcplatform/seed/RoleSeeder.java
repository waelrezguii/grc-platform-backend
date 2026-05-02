package com.project.grcplatform.seed;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.project.grcplatform.model.Role;
import com.project.grcplatform.repository.RoleRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Order(1)
public class RoleSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        seedRole("SUPER_ADMIN");
        seedRole("ADMIN");
        seedRole("MANAGER");
        seedRole("RSSI");
        seedRole("AUDITEUR");
    }

    private void seedRole(String name) {
        if (roleRepository.findByName(name).isEmpty()) {
            Role role = new Role();
            role.setName(name);
            roleRepository.save(role);
        }
    }
}