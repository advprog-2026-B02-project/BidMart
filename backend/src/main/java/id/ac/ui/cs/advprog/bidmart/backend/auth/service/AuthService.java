package id.ac.ui.cs.advprog.bidmart.backend.auth.service;

import id.ac.ui.cs.advprog.bidmart.backend.auth.entity.PasswordResetToken;
import id.ac.ui.cs.advprog.bidmart.backend.auth.repository.PasswordResetTokenRepository;
import id.ac.ui.cs.advprog.bidmart.backend.auth.security.JwtService;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.AuthResponse;
import id.ac.ui.cs.advprog.bidmart.backend.auth.entity.User;
import id.ac.ui.cs.advprog.bidmart.backend.auth.entity.RefreshToken;
import id.ac.ui.cs.advprog.bidmart.backend.auth.entity.EmailVerificationToken;
import id.ac.ui.cs.advprog.bidmart.backend.auth.repository.UserRepository;
import id.ac.ui.cs.advprog.bidmart.backend.auth.repository.RefreshTokenRepository;
import id.ac.ui.cs.advprog.bidmart.backend.auth.repository.EmailVerificationTokenRepository;
import id.ac.ui.cs.advprog.bidmart.backend.auth.config.AuthProperties;
import id.ac.ui.cs.advprog.bidmart.backend.auth.config.AppProperties;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final EmailVerificationTokenRepository verificationTokens;
    private final PasswordResetTokenRepository resetTokens;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final JwtService jwtService;
    private final EmailService emailService;
    private final AuthProperties authProps;
    private final AppProperties appProps;

    public AuthService(UserRepository users,
                       RefreshTokenRepository refreshTokens,
                       EmailVerificationTokenRepository verificationTokens,
                       PasswordResetTokenRepository resetTokens,
                       AuthProperties authProps,
                       AppProperties appProps,
                       EmailService emailService) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.verificationTokens = verificationTokens;
        this.resetTokens = resetTokens;
        this.authProps = authProps;
        this.appProps = appProps;
        this.jwtService = new JwtService(authProps);
        this.emailService = emailService;
    }

    @Transactional
    public void register(String email, String rawPassword, String displayName) {
        String normalized = email.toLowerCase().trim();

        Optional<User> existingUser = users.findByEmail(normalized);

        if (existingUser.isPresent()) {
            User u = existingUser.get();
            if (u.isEmailVerified()) {
                throw new IllegalArgumentException("Email already registered");
            }

            u.setPasswordHash(passwordEncoder.encode(rawPassword));
            u.setDisplayName(displayName);
            users.save(u);

            sendVerificationProcedure(u);
            return;
        }

        User u = new User();
        u.setEmail(normalized);
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setDisplayName(displayName);
        u.setEmailVerified(false);
        users.save(u);

        sendVerificationProcedure(u);
    }

    private void sendVerificationProcedure(User u) {
        verificationTokens.deleteByUserAndUsedAtIsNull(u);

        EmailVerificationToken t = new EmailVerificationToken();
        t.setUser(u);
        t.setToken(UUID.randomUUID().toString());

        // expired in 24 hours
        t.setExpiresAt(Instant.now().plusSeconds(60 * 60 * 24));
        verificationTokens.save(t);

        String link = appProps.getBaseUrl() + "/auth/verify?token=" + t.getToken();
        emailService.sendVerificationEmail(u.getEmail(), link);
    }

    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken t = verificationTokens.findByToken(token).orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (t.getUsedAt() != null) throw new IllegalArgumentException("Token already used");
        if (t.getExpiresAt().isBefore(Instant.now())) throw new IllegalArgumentException("Token expired");

        User u = t.getUser();
        u.setEmailVerified(true);
        t.setUsedAt(Instant.now());

        users.save(u);
        verificationTokens.save(t);
    }

    @Transactional
    public AuthResponse login(String email, String rawPassword) {
        User u = users.findByEmail(email.toLowerCase().trim()).orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(rawPassword, u.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        if (!u.isEmailVerified()) {
            throw new IllegalStateException("Email not verified");
        }

        String accessToken = jwtService.generateAccessToken(u.getId(), u.getEmail());

        RefreshToken rt = new RefreshToken();
        rt.setUser(u);
        rt.setToken(generateRefreshToken());
        rt.setExpiresAt(Instant.now().plusMillis(authProps.getRefreshTokenExpiration()));
        rt.setRevoked(false);

        refreshTokens.save(rt);

        return new AuthResponse(accessToken, rt.getToken());
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        RefreshToken rt = refreshTokens.findByToken(refreshToken).orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (rt.isRevoked()) throw new IllegalArgumentException("Refresh token revoked");
        if (rt.getExpiresAt().isBefore(Instant.now())) throw new IllegalArgumentException("Refresh token expired");

        User u = rt.getUser();
        String accessToken = jwtService.generateAccessToken(u.getId(), u.getEmail());
        return new AuthResponse(accessToken, refreshToken);
    }

    @Transactional
    public void logout(String refreshToken) {
        RefreshToken rt = refreshTokens.findByToken(refreshToken).orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        rt.setRevoked(true);
        refreshTokens.save(rt);
    }

    private static String generateRefreshToken() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Transactional
    public void forgotPassword(String email) {
        User u = users.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("Email not found"));

        resetTokens.deleteByUserAndUsedAtIsNull(u);

        PasswordResetToken t = new PasswordResetToken();
        t.setUser(u);
        t.setToken(UUID.randomUUID().toString());
        t.setExpiresAt(Instant.now().plusSeconds(3600)); // expired in 1 hour
        resetTokens.save(t);

        String link = appProps.getBaseUrl() + "/auth/reset?token=" + t.getToken();
        emailService.sendResetPasswordEmail(u.getEmail(), link);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken t = resetTokens.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid reset token"));

        if (t.getUsedAt() != null) {
            throw new IllegalArgumentException("Reset token already used");
        }
        if (t.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Reset token expired");
        }

        User u = t.getUser();
        u.setPasswordHash(passwordEncoder.encode(newPassword));
        users.save(u);

        t.setUsedAt(Instant.now());
        resetTokens.save(t);

        resetTokens.flush();
    }

    @Transactional(readOnly = true)
    public void validateResetToken(String token) {
        PasswordResetToken t = resetTokens.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid reset token"));

        if (t.getUsedAt() != null) {
            throw new IllegalArgumentException("Reset token already used");
        }
        if (t.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Reset token expired");
        }
    }
}