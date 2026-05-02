package com.project.grcplatform.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

public class JwtAuthToken implements Authentication {

    private final String userId;
    private final String role;
    private final String sessionId;
    private boolean authenticated = true;

    public JwtAuthToken(String userId, String role, String sessionId) {
        this.userId = userId;
        this.role = role;
        this.sessionId = sessionId;
    }

    public String getUserId()   { return userId; }
    public String getSessionId() { return sessionId; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override public Object getCredentials()  { return null; }
    @Override public Object getDetails()      { return null; }
    @Override public Object getPrincipal()    { return role; }
    @Override public boolean isAuthenticated() { return authenticated; }
    @Override public void setAuthenticated(boolean v) { this.authenticated = v; }
    @Override public String getName()         { return role; }
}
