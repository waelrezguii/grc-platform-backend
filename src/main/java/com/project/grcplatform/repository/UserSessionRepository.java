package com.project.grcplatform.repository;

import com.project.grcplatform.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, String> {

    Optional<UserSession> findByIdAndRevokedFalse(String id);

    List<UserSession> findAllByUserIdAndRevokedFalse(String userId);

    boolean existsByUserIdAndDeviceFingerprint(String userId, String deviceFingerprint);

    boolean existsByUserIdAndIpAddress(String userId, String ipAddress);

    @Modifying
    @Query("UPDATE UserSession s SET s.revoked = true WHERE s.userId = :userId")
    void revokeAllByUserId(@Param("userId") String userId);
}
