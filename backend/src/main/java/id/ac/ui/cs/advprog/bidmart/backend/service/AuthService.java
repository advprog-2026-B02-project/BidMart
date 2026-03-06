package id.ac.ui.cs.advprog.bidmart.backend.service;
import id.ac.ui.cs.advprog.bidmart.backend.security.JwtService;
import id.ac.ui.cs.advprog.bidmart.backend.dto.AuthResponse;
import id.ac.ui.cs.advprog.bidmart.backend.entity.User;
import id.ac.ui.cs.advprog.bidmart.backend.entity.RefreshToken;
import id.ac.ui.cs.advprog.bidmart.backend.entity.EmailVerificationToken;
import id.ac.ui.cs.advprog.bidmart.backend.repository.UserRepository;
import id.ac.ui.cs.advprog.bidmart.backend.repository.RefreshTokenRepository;
import id.ac.ui.cs.advprog.bidmart.backend.repository.EmailVerificationTokenRepository;
import id.ac.ui.cs.advprog.bidmart.backend.config.AuthProperties;
import id.ac.ui.cs.advprog.bidmart.backend.config.AppProperties;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final EmailVerificationTokenRepository verificationTokens;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final JwtService jwtService;
    private final AuthProperties authProps;
    private final AppProperties appProps;

    public AuthService(UserRepository users,
                       RefreshTokenRepository refreshTokens,
                       EmailVerificationTokenRepository verificationTokens,
                       AuthProperties authProps,
                       AppProperties appProps) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.verificationTokens = verificationTokens;
        this.authProps = authProps;
        this.appProps = appProps;
        this.jwtService = new JwtService(authProps);
    }

    @Transactional
    public void register(String email, String rawPassword) {
        String normalized = email.toLowerCase().trim();
        if (users.existsByEmail(normalized)) {
            throw new IllegalArgumentException("Email already registered");
        }

        User u = new User();
        u.setEmail(normalized);
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setEmailVerified(false);

        users.save(u);

        EmailVerificationToken t = new EmailVerificationToken();
        t.setUser(u);
        t.setToken(UUID.randomUUID().toString());
        t.setExpiresAt(Instant.now().plusSeconds(60 * 60 * 24));
        verificationTokens.save(t);
        String link = appProps.getBaseUrl() + "/auth/verify?token=" + t.getToken();
        System.out.println("[EMAIL-VERIFY] " + link);
    }

    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken t = verificationTokens.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

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
        User u = users.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

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
        RefreshToken rt = refreshTokens.findByToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (rt.isRevoked()) throw new IllegalArgumentException("Refresh token revoked");
        if (rt.getExpiresAt().isBefore(Instant.now())) throw new IllegalArgumentException("Refresh token expired");

        User u = rt.getUser();
        String accessToken = jwtService.generateAccessToken(u.getId(), u.getEmail());
        return new AuthResponse(accessToken, refreshToken);
    }

    @Transactional
    public void logout(String refreshToken) {
        RefreshToken rt = refreshTokens.findByToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        rt.setRevoked(true);
        refreshTokens.save(rt);
    }

    private static String generateRefreshToken() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}