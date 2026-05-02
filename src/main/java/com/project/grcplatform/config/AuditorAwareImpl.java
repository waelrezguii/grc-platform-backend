package com.project.grcplatform.config;

import com.project.grcplatform.model.User;
import com.project.grcplatform.repository.UserRepository;
import com.project.grcplatform.security.JwtAuthToken;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuditorAwareImpl implements AuditorAware<User> {

    private final UserRepository userRepository;

    public AuditorAwareImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<User> getCurrentAuditor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty(); // or return system user if needed
        }

        if (auth instanceof JwtAuthToken jwtAuthToken) {
            String userId = jwtAuthToken.getUserId();
            return userRepository.findById(userId);
        }

        // 🔥 Use email here (since your repo supports it)
        String email = auth.getName();
        return userRepository.findByEmail(email);
    }
}