package com.project.grcplatform.model;

import com.project.grcplatform.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_sessions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserSession extends BaseEntity {

    @Column(nullable = false)
    private String userId;

    private String deviceFingerprint;
    private String deviceName;
    private String ipAddress;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime lastActivityAt;

    @Builder.Default
    private Boolean revoked = false;
}
