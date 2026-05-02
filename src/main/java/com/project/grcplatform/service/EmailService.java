package com.project.grcplatform.service;

import com.project.grcplatform.constant.Language;
import com.project.grcplatform.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final MessageSource messageSource;

    @Value("${app.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    @Value("${app.backend.base-url:http://localhost:8080}")
    private String backendBaseUrl;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── OTP ──────────────────────────────────────────────────────────────────

    public void sendOtpEmail(String to, String firstname, String otp, Language language) {
        Locale locale = localeOf(language);
        send(to,
                msg("email.otp.subject", locale),
                msg("email.otp.body", locale, firstname, otp));
    }

    // ── Welcome ───────────────────────────────────────────────────────────────

    public void sendWelcomeEmail(String to, String firstname, String tempPassword, Language language) {
        Locale locale = localeOf(language);
        send(to,
                msg("email.welcome.subject", locale),
                msg("email.welcome.body", locale, firstname, to, tempPassword));
    }

    // ── New device / location alert (to user) ─────────────────────────────────

    public void sendNewDeviceAlert(User user, String ip, String device,
                                   boolean newDevice, boolean newIp) {
        Locale locale = localeOf(user.getLanguage());
        String detail = buildDetail(newDevice, newIp, ip, locale);
        send(user.getEmail(),
                msg("email.newdevice.subject", locale),
                msg("email.newdevice.body", locale,
                        user.getFirstname(),
                        detail,
                        LocalDateTime.now().format(FMT),
                        device != null ? device : "Unknown",
                        ip     != null ? ip     : "Unknown"));
    }

    // ── New device / location alert (to admin) ────────────────────────────────

    public void sendAdminSecurityAlert(User admin, User targetUser, String ip, String device,
                                       boolean newDevice, boolean newIp) {
        Locale locale = localeOf(admin.getLanguage());
        String detail = buildDetail(newDevice, newIp, ip, locale);
        send(admin.getEmail(),
                msg("email.admin.security.subject", locale, targetUser.getEmail()),
                msg("email.admin.security.body", locale,
                        admin.getFirstname(),
                        targetUser.getEmail(),
                        detail,
                        LocalDateTime.now().format(FMT),
                        device != null ? device : "Unknown",
                        ip     != null ? ip     : "Unknown"));
    }

    // ── Password reset ────────────────────────────────────────────────────────

    public void sendPasswordResetEmail(String to, String firstname, String resetToken, Language language) {
        Locale locale = localeOf(language);
        String resetLink  = frontendBaseUrl + "/reset-password?token=" + resetToken;
        String cancelLink = backendBaseUrl + "/api/auth/cancel-reset?token=" + resetToken;
        send(to,
                msg("email.reset.subject", locale),
                msg("email.reset.body", locale, firstname, resetLink, cancelLink));
    }

    public void sendPasswordResetConfirmation(String to, String firstname, Language language) {
        Locale locale = localeOf(language);
        send(to,
                msg("email.reset.confirmed.subject", locale),
                msg("email.reset.confirmed.body", locale, firstname));
    }

    public void sendResetCancelledConfirmation(String to, String firstname, Language language) {
        Locale locale = localeOf(language);
        send(to,
                msg("email.reset.cancelled.subject", locale),
                msg("email.reset.cancelled.body", locale, firstname));
    }

    // ── Scanner sync failure alert ────────────────────────────────────────────

    public void sendSyncFailureAlert(User admin, String scannerName, String scannerType, String error) {
        Locale locale = localeOf(admin.getLanguage());
        send(admin.getEmail(),
                msg("email.sync.failure.subject", locale, scannerName),
                msg("email.sync.failure.body", locale,
                        admin.getFirstname(), scannerName, scannerType, error));
    }

    // ── Asset end-of-life alert ───────────────────────────────────────────────

    public void sendEndOfLifeAlert(User owner, String assetRef, String assetName,
                                   LocalDate endOfLifeDate, int daysRemaining) {
        Locale locale = localeOf(owner.getLanguage());
        if (daysRemaining <= 0) {
            send(owner.getEmail(),
                    msg("email.eol.past.subject", locale, assetRef),
                    msg("email.eol.past.body", locale,
                            owner.getFirstname(), assetRef, assetName, endOfLifeDate));
        } else {
            send(owner.getEmail(),
                    msg("email.eol.approaching.subject", locale, assetRef),
                    msg("email.eol.approaching.body", locale,
                            owner.getFirstname(), assetRef, assetName, endOfLifeDate, daysRemaining));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String buildDetail(boolean newDevice, boolean newIp, String ip, Locale locale) {
        if (newDevice && newIp) return msg("email.newdevice.detail.both",   locale, ip);
        if (newDevice)          return msg("email.newdevice.detail.device", locale);
        return                         msg("email.newdevice.detail.ip",     locale, ip);
    }

    private String msg(String key, Locale locale, Object... args) {
        return messageSource.getMessage(key, args, locale);
    }

    private Locale localeOf(Language language) {
        return language != null ? language.toLocale() : Locale.ENGLISH;
    }

    private void send(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
