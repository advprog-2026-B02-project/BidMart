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
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.email = email;
        request.password = rawPassword;
        request.displayName = displayName;
        registerAndReturn(request);
    }

    @Transactional
    public UserResponseDTO registerAndReturn(RegisterRequestDTO request) {
        String normalized = request.email.toLowerCase().trim();

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
        u.setEmailVerified(true);
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

        String link = buildFrontendLink("/auth/verify", t.getToken());
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
        Object loginResult = loginWithDesign(newLoginRequest(email, rawPassword), null);
        if (loginResult instanceof PartialLoginResponseDTO) {
            throw new IllegalStateException("2FA verification required");
        }
        LoginSuccessResponseDTO success = (LoginSuccessResponseDTO) loginResult;
        return new AuthResponse(success.accessToken, success.refreshToken);
    }

    @Transactional
    public Object loginWithDesign(LoginRequestDTO request, HttpServletRequest servletRequest) {
        User u = users.findByEmail(request.email.toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password, u.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        if (!u.isEmailVerified()) {
            throw new IllegalStateException("Email not verified");
        }

        if (u.isTwoFactorEnabled()) {
            return createPartialSession(u);
        }

        return createLoginSuccess(u, servletRequest);
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

    private String buildFrontendLink(String path, String token) {
        String frontend = appProps.getFrontendUrl();
        if (frontend == null || frontend.isBlank()) {
            frontend = "http://localhost:3000";
        }
        return frontend + path + "?token=" + token;
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

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return users.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Transactional(readOnly = true)
    public User getUserById(UUID userId) {
        return users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private LoginRequestDTO newLoginRequest(String email, String password) {
        LoginRequestDTO req = new LoginRequestDTO();
        req.email = email;
        req.password = password;
        return req;
    }

    private Object createPartialSession(User user) {
        String method = resolveTwoFactorMethod(user);

        PartialAuthSession partial = new PartialAuthSession();
        partial.setUser(user);
        partial.setPartialToken(generateRefreshToken());
        partial.setMethods(method);
        partial.setExpiresAt(Instant.now().plusSeconds(300));

        if ("EMAIL".equals(method)) {
            String code = generateNumericCode();
            partial.setEmailOtpHash(passwordEncoder.encode(code));
            partial.setEmailOtpExpiresAt(Instant.now().plusSeconds(300));
            emailService.sendTwoFactorCodeEmail(user.getEmail(), code);
        }

        partialAuthSessions.save(partial);

        return new PartialLoginResponseDTO(partial.getPartialToken(), true, List.of(method), 300);
    }

    private String resolveTwoFactorMethod(User user) {
        String configured = user.getTwoFactorMethod();
        if (configured != null && !configured.isBlank()) {
            return configured.toUpperCase(Locale.ROOT);
        }
        if (user.getTwoFactorSecret() != null && !user.getTwoFactorSecret().isBlank()) {
            return "TOTP";
        }
        return "EMAIL";
    }

    private boolean supportsMethod(String methods, String method) {
        if (methods == null || methods.isBlank()) {
            return false;
        }
        String target = method.toUpperCase(Locale.ROOT);
        return Arrays.stream(methods.split(","))
                .map(String::trim)
                .map(s -> s.toUpperCase(Locale.ROOT))
                .anyMatch(target::equals);
    }

    private String generateNumericCode() {
        SecureRandom random = new SecureRandom();
        int number = random.nextInt(900000) + 100000;
        return String.valueOf(number);
    }

    private LoginSuccessResponseDTO createLoginSuccess(User user, HttpServletRequest servletRequest) {
        RefreshToken session = new RefreshToken();
        session.setUser(user);
        session.setToken(generateRefreshToken());
        session.setExpiresAt(Instant.now().plusMillis(authProps.getRefreshTokenExpiration()));
        session.setRevoked(false);
        session.setDevice(extractDevice(servletRequest));
        session.setIpAddress(extractIp(servletRequest));
        session.setLastActive(Instant.now());
        refreshTokens.save(session);

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), session.getId(), user.getRolesList());

        return new LoginSuccessResponseDTO(
                accessToken,
                session.getToken(),
                authProps.getAccessTokenExpiration() / 1000,
                toUserResponse(user)
        );
    }

    private String extractDevice(HttpServletRequest request) {
        if (request == null) {
            return "Unknown device";
        }
        String ua = request.getHeader("User-Agent");
        if (ua == null || ua.isBlank()) {
            return "Unknown device";
        }
        return ua.length() > 180 ? ua.substring(0, 180) : ua;
    }

    private String extractIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private List<String> generateBackupCodes() {
        return List.of(
                shortCode(),
                shortCode(),
                shortCode(),
                shortCode(),
                shortCode()
        );
    }

    private String shortCode() {
        // 6 bytes in Base64 URL-safe (without padding) produces exactly 8 chars.
        byte[] bytes = new byte[6];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).toUpperCase(Locale.ROOT);
    }

    private UserResponseDTO toUserResponse(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.isEmailVerified(),
                user.getCreatedAt(),
                user.getRolesList()
        );
    }

    private RoleResponseDTO toRoleResponse(Role role) {
        List<String> permissions = List.of();
        if (role.getPermissions() != null && !role.getPermissions().isBlank()) {
            permissions = List.of(role.getPermissions().split(","));
        }
        return new RoleResponseDTO(role.getId(), role.getName(), permissions);
    }
}