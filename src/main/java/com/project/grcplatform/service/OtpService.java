package com.project.grcplatform.service;

import com.project.grcplatform.constant.ErrorCode;
import com.project.grcplatform.exception.AppException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private record OtpEntry(String userId, String otp, LocalDateTime expiresAt) {}

    private final ConcurrentHashMap<String, OtpEntry> store = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    private static final int OTP_EXPIRY_MINUTES = 5;

    /** Generates a 6-digit OTP, stores it, returns the pendingToken to give the frontend. */
    public String generateAndStore(String userId) {
        // Invalidate any existing pending token for this user
        store.entrySet().removeIf(e -> e.getValue().userId().equals(userId));

        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
        String pendingToken = UUID.randomUUID().toString();
        store.put(pendingToken, new OtpEntry(userId, otp, LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES)));
        return pendingToken;
    }

    /** Returns the raw OTP for a pending token (used by EmailService). */
    public String getOtp(String pendingToken) {
        OtpEntry entry = store.get(pendingToken);
        return entry != null ? entry.otp() : null;
    }

    /**
     * Validates the OTP. On success returns the userId and invalidates the token.
     * Throws INVALID_OTP or OTP_EXPIRED on failure.
     */
    public String validateAndConsume(String pendingToken, String otp) {
        OtpEntry entry = store.get(pendingToken);

        if (entry == null || !entry.otp().equals(otp)) {
            throw new AppException(ErrorCode.INVALID_OTP);
        }
        if (LocalDateTime.now().isAfter(entry.expiresAt())) {
            store.remove(pendingToken);
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }

        store.remove(pendingToken);
        return entry.userId();
    }
}
