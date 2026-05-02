package com.project.grcplatform.repository;

import com.project.grcplatform.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {

    Optional<PasswordResetToken> findByIdAndUsedFalseAndCancelledFalse(String id);
}
