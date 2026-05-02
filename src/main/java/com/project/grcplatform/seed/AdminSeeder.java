package com.project.grcplatform.seed;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.project.grcplatform.model.Role;
import com.project.grcplatform.model.User;
import com.project.grcplatform.repository.RoleRepository;
import com.project.grcplatform.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Order(3)
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {

        String adminEmail = "rezguiwael@hotmail.com";

        if (userRepository.findByEmail(adminEmail).isEmpty()) {

            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseThrow(() -> new RuntimeException("ADMIN role not found"));

            User admin = User.builder()
                    .email(adminEmail)
                    .firstname("Wael")
                    .lastname("Rezgui")
                    .password(passwordEncoder.encode("Admin1234!"))
                    .role(adminRole)
                    .isActive(true)
                    .build();

            userRepository.save(admin);
        }
    }
}