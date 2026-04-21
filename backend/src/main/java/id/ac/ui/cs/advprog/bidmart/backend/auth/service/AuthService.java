package id.ac.ui.cs.advprog.bidmart.backend.auth.service;

import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.AuthResponse;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.ChangePasswordRequestDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.LoginRequestDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.LoginSuccessResponseDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.PartialLoginResponseDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.RegisterRequestDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.RoleRequestDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.RoleResponseDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.SessionResponseDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.TwoFactorSetupResponseDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.TwoFactorVerifyRequestDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.UserResponseDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.entity.PasswordResetToken;
import id.ac.ui.cs.advprog.bidmart.backend.auth.entity.EmailVerificationToken;
import id.ac.ui.cs.advprog.bidmart.backend.auth.entity.PartialAuthSession;
import id.ac.ui.cs.advprog.bidmart.backend.auth.entity.RefreshToken;
import id.ac.ui.cs.advprog.bidmart.backend.auth.entity.Role;
import id.ac.ui.cs.advprog.bidmart.backend.auth.entity.User;
import id.ac.ui.cs.advprog.bidmart.backend.auth.entity.UserStatus;
import id.ac.ui.cs.advprog.bidmart.backend.auth.repository.EmailVerificationTokenRepository;
import id.ac.ui.cs.advprog.bidmart.backend.auth.repository.PartialAuthSessionRepository;
import id.ac.ui.cs.advprog.bidmart.backend.auth.repository.PasswordResetTokenRepository;
import id.ac.ui.cs.advprog.bidmart.backend.auth.repository.RefreshTokenRepository;
import id.ac.ui.cs.advprog.bidmart.backend.auth.repository.RoleRepository;
import id.ac.ui.cs.advprog.bidmart.backend.auth.repository.UserRepository;
import id.ac.ui.cs.advprog.bidmart.backend.auth.security.JwtService;
import id.ac.ui.cs.advprog.bidmart.backend.auth.config.AppProperties;
import id.ac.ui.cs.advprog.bidmart.backend.auth.config.AuthProperties;
import id.ac.ui.cs.advprog.bidmart.common.event.UserRoleChangedEvent;
import id.ac.ui.cs.advprog.bidmart.common.event.UserSuspendedEvent;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEvent;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final RoleRepository roles;
    private final EmailVerificationTokenRepository verificationTokens;
    private final PasswordResetTokenRepository resetTokens;
    private final PartialAuthSessionRepository partialAuthSessions;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final TotpService totpService;
    private final ApplicationEventPublisher eventPublisher;

    private final JwtService jwtService;
    private final EmailService emailService;
    private final AuthProperties authProps;
    private final AppProperties appProps;

    @Autowired
    public AuthService(UserRepository users,
                       RefreshTokenRepository refreshTokens,
                       RoleRepository roles,
                       EmailVerificationTokenRepository verificationTokens,
                       PasswordResetTokenRepository resetTokens,
                       PartialAuthSessionRepository partialAuthSessions,
                       AuthProperties authProps,
                       AppProperties appProps,
                       EmailService emailService,
                       TotpService totpService,
                       ApplicationEventPublisher eventPublisher) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.roles = roles;
        this.verificationTokens = verificationTokens;
        this.resetTokens = resetTokens;
        this.partialAuthSessions = partialAuthSessions;
        this.authProps = authProps;
        this.appProps = appProps;
        this.jwtService = new JwtService(authProps);
        this.emailService = emailService;
        this.totpService = totpService;
        this.eventPublisher = eventPublisher;
    }

    // Backward-compatible constructor for older tests.
    public AuthService(UserRepository users,
                       RefreshTokenRepository refreshTokens,
                       EmailVerificationTokenRepository verificationTokens,
                       PasswordResetTokenRepository resetTokens,
                       AuthProperties authProps,
                       AppProperties appProps,
                       EmailService emailService) {
        this(users,
                refreshTokens,
            null,
                verificationTokens,
                resetTokens,
                null,
                authProps,
                appProps,
                emailService,
                new TotpService(),
                new ApplicationEventPublisher() {
                    @Override
                    public void publishEvent(Object event) {
                        // no-op for legacy tests
                    }

                    @Override
                    public void publishEvent(ApplicationEvent event) {
                        // no-op for legacy tests
                    }
                });
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

            u.setPasswordHash(passwordEncoder.encode(request.password));
            u.setDisplayName(request.displayName);
            users.save(u);

            sendVerificationProcedure(u);
            return toUserResponse(u);
        }

        User u = new User();
        u.setEmail(normalized);
        u.setPasswordHash(passwordEncoder.encode(request.password));
        u.setDisplayName(request.displayName);
        u.setEmailVerified(false);
        u.setRolesList(List.of("BUYER", "SELLER"));
        u.setStatus(UserStatus.ACTIVE);
        users.save(u);

        sendVerificationProcedure(u);
        return toUserResponse(u);
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
        if (u.getStatus() == UserStatus.SUSPENDED) {
            throw new IllegalStateException("Account suspended");
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
        LoginSuccessResponseDTO refreshed = refreshWithDesign(refreshToken);
        return new AuthResponse(refreshed.accessToken, refreshed.refreshToken);
    }

    @Transactional
    public LoginSuccessResponseDTO refreshWithDesign(String refreshToken) {
        RefreshToken rt = refreshTokens.findByToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (rt.isRevoked()) throw new IllegalArgumentException("Refresh token revoked");
        if (rt.getExpiresAt().isBefore(Instant.now())) throw new IllegalArgumentException("Refresh token expired");

        User u = rt.getUser();
        rt.setLastActive(Instant.now());
        refreshTokens.save(rt);

        String accessToken = jwtService.generateAccessToken(u.getId(), u.getEmail(), rt.getId(), u.getRolesList());
        return new LoginSuccessResponseDTO(accessToken, refreshToken, authProps.getAccessTokenExpiration() / 1000, toUserResponse(u));
    }

    @Transactional
    public void logout(String refreshToken) {
        RefreshToken rt = refreshTokens.findByToken(refreshToken).orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        rt.setRevoked(true);
        refreshTokens.save(rt);
    }

    @Transactional
    public LoginSuccessResponseDTO verifyTwoFactor(TwoFactorVerifyRequestDTO request, HttpServletRequest servletRequest) {
        PartialAuthSession partial = partialAuthSessions.findByPartialToken(request.partialToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid partial token"));

        if (partial.isUsed()) {
            throw new IllegalArgumentException("Partial token already used");
        }
        if (partial.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Partial token expired");
        }

        User user = partial.getUser();
        String method = request.method.trim().toUpperCase(Locale.ROOT);

        if (!"TOTP".equals(method)) {
            throw new IllegalArgumentException("Unsupported 2FA method");
        }

        boolean verified = totpService.verifyCode(user.getTwoFactorSecret(), request.code);

        if (!verified) {
            throw new IllegalArgumentException("Invalid 2FA code");
        }

        partial.setUsed(true);
        partialAuthSessions.save(partial);

        return createLoginSuccess(user, servletRequest);
    }

    @Transactional
    public TwoFactorSetupResponseDTO setupTwoFactor(User user, String method) {
        if (!"TOTP".equalsIgnoreCase(method)) {
            throw new IllegalArgumentException("Only TOTP is supported");
        }

        String secret = totpService.generateBase32Secret();
        List<String> backupCodes = generateBackupCodes();

        user.setTwoFactorTempSecret(secret);
        user.setTwoFactorBackupCodes(String.join(",", backupCodes));
        user.setTwoFactorMethod("TOTP");
        users.save(user);

        return new TwoFactorSetupResponseDTO(secret, null, backupCodes);
    }

    @Transactional
    public void confirmTwoFactor(User user, String code) {
        String tempSecret = user.getTwoFactorTempSecret();
        if (tempSecret == null || tempSecret.isBlank()) {
            throw new IllegalArgumentException("2FA setup has not been started");
        }

        if (!totpService.verifyCode(tempSecret, code)) {
            throw new IllegalArgumentException("Invalid 2FA code");
        }

        user.setTwoFactorSecret(tempSecret);
        user.setTwoFactorTempSecret(null);
        user.setTwoFactorEnabled(true);
        user.setTwoFactorMethod("TOTP");
        users.save(user);
    }

    @Transactional
    public void disableTwoFactor(User user, String password) {
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid password");
        }
        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        user.setTwoFactorTempSecret(null);
        user.setTwoFactorBackupCodes(null);
        user.setTwoFactorMethod(null);
        users.save(user);
    }

    @Transactional(readOnly = true)
    public List<SessionResponseDTO> getActiveSessions(User user, String currentSessionId) {
        List<RefreshToken> sessions = refreshTokens.findByUserAndRevokedFalseOrderByCreatedAtDesc(user);
        return sessions.stream()
                .map(s -> new SessionResponseDTO(
                        s.getId(),
                        s.getDevice(),
                        s.getIpAddress(),
                        s.getLastActive(),
                        currentSessionId != null && currentSessionId.equals(s.getId().toString())
                ))
                .toList();
    }

    @Transactional
    public void revokeSession(User user, UUID sessionId) {
        RefreshToken session = refreshTokens.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        session.setRevoked(true);
        refreshTokens.save(session);
    }

    @Transactional
    public void changePassword(User user, ChangePasswordRequestDTO request) {
        if (!passwordEncoder.matches(request.currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password invalid");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword));
        users.save(user);
    }

    @Transactional
    public User updateProfile(User user, String displayName, String avatarUrl) {
        user.setDisplayName(displayName);
        user.setAvatarUrl(avatarUrl);
        return users.save(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> adminListUsers(String search, String role, String status, int page, int size) {
        UserStatus filterStatus = null;
        if (status != null && !status.isBlank()) {
            filterStatus = UserStatus.valueOf(status.toUpperCase(Locale.ROOT));
        }

        List<User> candidates = users.searchUsers(search, filterStatus);
        List<User> filtered = candidates.stream()
                .filter(u -> role == null || role.isBlank() || u.getRolesList().contains(role.toUpperCase(Locale.ROOT)))
                .sorted(Comparator.comparing(User::getCreatedAt).reversed())
                .collect(Collectors.toList());

        int from = Math.min(page * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        return filtered.subList(from, to).stream().map(this::toUserResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponseDTO adminGetUser(UUID userId) {
        return toUserResponse(getUserById(userId));
    }

    @Transactional
    public UserResponseDTO adminUpdateUserStatus(UUID userId, String status, String reason) {
        User user = getUserById(userId);
        UserStatus newStatus = UserStatus.valueOf(status.toUpperCase(Locale.ROOT));
        user.setStatus(newStatus);
        users.save(user);

        if (newStatus == UserStatus.SUSPENDED) {
            eventPublisher.publishEvent(new UserSuspendedEvent(user.getId(), reason, Instant.now()));
            refreshTokens.revokeAllByUser(user);
        }

        return toUserResponse(user);
    }

    @Transactional
    public UserResponseDTO adminUpdateUserRoles(UUID userId, List<String> roles) {
        User user = getUserById(userId);
        user.setRolesList(roles);
        users.save(user);
        eventPublisher.publishEvent(new UserRoleChangedEvent(user.getId(), user.getRolesList(), Instant.now()));
        return toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public List<RoleResponseDTO> adminListRoles() {
        if (roles == null) {
            return List.of();
        }
        return roles.findAll().stream()
                .map(this::toRoleResponse)
                .toList();
    }

    @Transactional
    public RoleResponseDTO adminCreateRole(RoleRequestDTO req) {
        if (roles == null) {
            throw new IllegalStateException("Role repository unavailable");
        }
        if (roles.findByNameIgnoreCase(req.name).isPresent()) {
            throw new IllegalArgumentException("Role already exists");
        }

        Role role = new Role();
        role.setName(req.name.trim().toUpperCase(Locale.ROOT));
        role.setPermissions(String.join(",", req.permissions));
        roles.save(role);
        return toRoleResponse(role);
    }

    @Transactional
    public RoleResponseDTO adminUpdateRole(UUID roleId, RoleRequestDTO req) {
        if (roles == null) {
            throw new IllegalStateException("Role repository unavailable");
        }
        Role role = roles.findById(roleId).orElseThrow(() -> new IllegalArgumentException("Role not found"));
        role.setName(req.name.trim().toUpperCase(Locale.ROOT));
        role.setPermissions(String.join(",", req.permissions));
        roles.save(role);
        return toRoleResponse(role);
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

        String link = buildFrontendLink("/auth/reset", t.getToken());
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
        PartialAuthSession partial = new PartialAuthSession();
        partial.setUser(user);
        partial.setPartialToken(generateRefreshToken());
        partial.setMethods("TOTP");
        partial.setExpiresAt(Instant.now().plusSeconds(300));

        partialAuthSessions.save(partial);

        return new PartialLoginResponseDTO(partial.getPartialToken(), true, List.of("TOTP"), 300);
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

    @Transactional(readOnly = true)
    public void validateUser(UUID userId) {
        if (!users.existsById(userId)) {
            throw new IllegalArgumentException("ID tidak terdaftar di sistem.");
        }

        User u = users.findById(userId).get();
        if (u.getStatus() == UserStatus.SUSPENDED) {
            throw new IllegalStateException("User ini telah di-suspend.");
        }
    }
}