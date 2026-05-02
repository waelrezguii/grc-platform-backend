package com.project.grcplatform.security;

import com.project.grcplatform.repository.UserSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final UserSessionRepository userSessionRepository;
    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtUtils.validateToken(token)) {
                String sessionId = jwtUtils.getSessionId(token);

                // Reject if session was revoked (logout)
                if (sessionId != null && userSessionRepository.findByIdAndRevokedFalse(sessionId).isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                String userId = jwtUtils.getUserId(token);
                String role   = jwtUtils.getRole(token);
                SecurityContextHolder.getContext().setAuthentication(
                        new JwtAuthToken(userId, role, sessionId)
                );
            }
        }

        filterChain.doFilter(request, response);
    }
}
