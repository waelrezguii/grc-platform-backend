package com.project.grcplatform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "security.login")
@Data
public class LoginSecurityProperties {
    private int maxAttempts = 5;
    private int lockoutMinutes = 15;
}
