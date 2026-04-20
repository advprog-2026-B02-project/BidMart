package id.ac.ui.cs.advprog.bidmart.backend.auth.service;

import id.ac.ui.cs.advprog.bidmart.backend.auth.config.AppProperties;
import id.ac.ui.cs.advprog.bidmart.backend.auth.config.AuthProperties;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.ChangePasswordRequestDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.LoginRequestDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.LoginSuccessResponseDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.PartialLoginResponseDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.RoleRequestDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.TwoFactorVerifyRequestDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.UserResponseDTO;
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
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceDesignTest {

    @Mock
    private UserRepository users;
    @Mock
    private RefreshTokenRepository refreshTokens;
    @Mock
    private RoleRepository roles;
    @Mock
    private EmailVerificationTokenRepository verificationTokens;
    @Mock
    private PasswordResetTokenRepository resetTokens;
    @Mock
    private PartialAuthSessionRepository partialAuthSessions;
    @Mock
    private EmailService emailService;
    @Mock
    private TotpService totpService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private HttpServletRequest servletRequest;

    private AuthService authService;
    private User user;

    @BeforeEach
    void setUp() {
        AuthProperties authProperties = new AuthProperties();
        authProperties.setSecret("my-super-secret-key-that-is-at-least-32-bytes");
        authProperties.setAccessTokenExpiration(3600000L);
        authProperties.setRefreshTokenExpiration(7200000L);

        AppProperties appProperties = new AppProperties();
        appProperties.setFrontendUrl("http://frontend.local");

        authService = new AuthService(
                users,
                refreshTokens,
                roles,
                verificationTokens,
                resetTokens,
                partialAuthSessions,
                authProperties,
                appProperties,
                emailService,
                totpService,
                eventPublisher
        );

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        user = new User();
        user.setEmail("user@example.com");
        user.setDisplayName("User");
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setRolesList(List.of("BUYER"));
        user.setPasswordHash(encoder.encode("password123"));
    }

    @Test
    void loginWithDesign_TwoFactorEnabledReturnsPartial() {
        user.setTwoFactorEnabled(true);
        LoginRequestDTO request = new LoginRequestDTO();
        request.email = "user@example.com";
        request.password = "password123";

        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(partialAuthSessions.save(any(PartialAuthSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Object response = authService.loginWithDesign(request, null);

        assertTrue(response instanceof PartialLoginResponseDTO);
        PartialLoginResponseDTO dto = (PartialLoginResponseDTO) response;
        assertTrue(dto.requires2FA);
        assertEquals(1, dto.methods.size());
    }

    @Test
    void login_WhenTwoFactorEnabledThrowsInLegacyLogin() {
        user.setTwoFactorEnabled(true);
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(partialAuthSessions.save(any(PartialAuthSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> authService.login("user@example.com", "password123"));
        assertEquals("2FA verification required", ex.getMessage());
    }

    @Test
    void loginWithDesign_InvalidCredentialsWhenUserMissing() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.email = "missing@example.com";
        request.password = "password123";
        when(users.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.loginWithDesign(request, null));
        assertEquals("Invalid credentials", ex.getMessage());
    }

    @Test
    void loginWithDesign_SuspendedThrows() {
        user.setStatus(UserStatus.SUSPENDED);
        LoginRequestDTO request = new LoginRequestDTO();
        request.email = "user@example.com";
        request.password = "password123";
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> authService.loginWithDesign(request, null));
        assertEquals("Account suspended", ex.getMessage());
    }

    @Test
    void verifyTwoFactor_Success() {
        PartialAuthSession partial = new PartialAuthSession();
        partial.setUser(user);
        partial.setPartialToken("pt");
        partial.setUsed(false);
        partial.setExpiresAt(Instant.now().plusSeconds(120));

        TwoFactorVerifyRequestDTO req = new TwoFactorVerifyRequestDTO();
        req.partialToken = "pt";
        req.method = "TOTP";
        req.code = "123456";

        when(partialAuthSessions.findByPartialToken("pt")).thenReturn(Optional.of(partial));
        when(totpService.verifyCode(user.getTwoFactorSecret(), "123456")).thenReturn(true);
        when(refreshTokens.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(servletRequest.getHeader("User-Agent")).thenReturn("Mozilla");
        when(servletRequest.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1");

        LoginSuccessResponseDTO response = authService.verifyTwoFactor(req, servletRequest);

        assertNotNull(response.accessToken);
        assertNotNull(response.refreshToken);
        assertFalse(response.refreshToken.isBlank());
        verify(partialAuthSessions).save(partial);
    }

    @Test
    void verifyTwoFactor_UnsupportedMethod() {
        PartialAuthSession partial = new PartialAuthSession();
        partial.setUser(user);
        partial.setUsed(false);
        partial.setExpiresAt(Instant.now().plusSeconds(120));

        TwoFactorVerifyRequestDTO req = new TwoFactorVerifyRequestDTO();
        req.partialToken = "pt";
        req.method = "EMAIL";
        req.code = "123456";

        when(partialAuthSessions.findByPartialToken("pt")).thenReturn(Optional.of(partial));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.verifyTwoFactor(req, servletRequest));
        assertEquals("Unsupported 2FA method", ex.getMessage());
    }

        @Test
        void verifyTwoFactor_InvalidTokenUsedExpiredAndWrongCode() {
        TwoFactorVerifyRequestDTO req = new TwoFactorVerifyRequestDTO();
        req.partialToken = "pt";
        req.method = "TOTP";
        req.code = "000000";

        when(partialAuthSessions.findByPartialToken("pt")).thenReturn(Optional.empty());
        assertEquals("Invalid partial token", assertThrows(IllegalArgumentException.class,
            () -> authService.verifyTwoFactor(req, servletRequest)).getMessage());

        PartialAuthSession used = new PartialAuthSession();
        used.setUser(user);
        used.setUsed(true);
        used.setExpiresAt(Instant.now().plusSeconds(30));
        when(partialAuthSessions.findByPartialToken("pt")).thenReturn(Optional.of(used));
        assertEquals("Partial token already used", assertThrows(IllegalArgumentException.class,
            () -> authService.verifyTwoFactor(req, servletRequest)).getMessage());

        PartialAuthSession expired = new PartialAuthSession();
        expired.setUser(user);
        expired.setUsed(false);
        expired.setExpiresAt(Instant.now().minusSeconds(1));
        when(partialAuthSessions.findByPartialToken("pt")).thenReturn(Optional.of(expired));
        assertEquals("Partial token expired", assertThrows(IllegalArgumentException.class,
            () -> authService.verifyTwoFactor(req, servletRequest)).getMessage());

        PartialAuthSession valid = new PartialAuthSession();
        valid.setUser(user);
        valid.setUsed(false);
        valid.setExpiresAt(Instant.now().plusSeconds(30));
        when(partialAuthSessions.findByPartialToken("pt")).thenReturn(Optional.of(valid));
        when(totpService.verifyCode(user.getTwoFactorSecret(), "000000")).thenReturn(false);
        assertEquals("Invalid 2FA code", assertThrows(IllegalArgumentException.class,
            () -> authService.verifyTwoFactor(req, servletRequest)).getMessage());
        }

    @Test
    void setupConfirmDisableTwoFactor_Flow() {
        when(users.save(user)).thenReturn(user);
        when(totpService.generateBase32Secret()).thenReturn("JBSWY3DPEHPK3PXP");
        when(totpService.verifyCode("JBSWY3DPEHPK3PXP", "123456")).thenReturn(true);

        authService.setupTwoFactor(user, "totp");

        assertEquals("JBSWY3DPEHPK3PXP", user.getTwoFactorTempSecret());
        assertEquals("TOTP", user.getTwoFactorMethod());
        assertNotNull(user.getTwoFactorBackupCodes());

        authService.confirmTwoFactor(user, "123456");
        assertTrue(user.isTwoFactorEnabled());
        assertEquals("JBSWY3DPEHPK3PXP", user.getTwoFactorSecret());

        authService.disableTwoFactor(user, "password123");
        assertFalse(user.isTwoFactorEnabled());
        assertEquals(null, user.getTwoFactorSecret());
    }

    @Test
    void setupTwoFactor_InvalidMethod() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.setupTwoFactor(user, "email"));
        assertEquals("Only TOTP is supported", ex.getMessage());
    }

    @Test
    void confirmAndDisableTwoFactor_InvalidConditions() {
        user.setTwoFactorTempSecret(null);
        IllegalArgumentException noSetup = assertThrows(IllegalArgumentException.class,
                () -> authService.confirmTwoFactor(user, "123456"));
        assertEquals("2FA setup has not been started", noSetup.getMessage());

        user.setTwoFactorTempSecret("JBSWY3DPEHPK3PXP");
        when(totpService.verifyCode("JBSWY3DPEHPK3PXP", "000000")).thenReturn(false);
        IllegalArgumentException invalidCode = assertThrows(IllegalArgumentException.class,
                () -> authService.confirmTwoFactor(user, "000000"));
        assertEquals("Invalid 2FA code", invalidCode.getMessage());

        IllegalArgumentException invalidPassword = assertThrows(IllegalArgumentException.class,
                () -> authService.disableTwoFactor(user, "wrongpass"));
        assertEquals("Invalid password", invalidPassword.getMessage());
    }

    @Test
    void confirmTwoFactor_BlankTempSecretThrows() {
        user.setTwoFactorTempSecret("   ");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.confirmTwoFactor(user, "123456"));
        assertEquals("2FA setup has not been started", ex.getMessage());
    }

    @Test
    void getActiveSessionsAndRevokeSession() {
        RefreshToken token = new RefreshToken();
        ReflectionTestUtils.setField(token, "id", UUID.randomUUID());
        token.setUser(user);
        token.setToken("rt");
        token.setDevice("Chrome");
        token.setIpAddress("127.0.0.1");
        token.setLastActive(Instant.now());

        when(refreshTokens.findByUserAndRevokedFalseOrderByCreatedAtDesc(user)).thenReturn(List.of(token));

        List<?> sessions = authService.getActiveSessions(user, null);
        assertEquals(1, sessions.size());

        UUID sessionId = UUID.randomUUID();
        when(refreshTokens.findByIdAndUser(sessionId, user)).thenReturn(Optional.of(token));
        authService.revokeSession(user, sessionId);
        verify(refreshTokens).save(token);

        when(refreshTokens.findByIdAndUser(sessionId, user)).thenReturn(Optional.empty());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.revokeSession(user, sessionId));
        assertEquals("Session not found", ex.getMessage());
    }

    @Test
    void getActiveSessions_CurrentSessionMatchAndMismatch() {
        RefreshToken first = new RefreshToken();
        UUID firstId = UUID.randomUUID();
        ReflectionTestUtils.setField(first, "id", firstId);
        first.setUser(user);
        first.setDevice("Chrome");
        first.setIpAddress("10.0.0.1");
        first.setLastActive(Instant.now());

        RefreshToken second = new RefreshToken();
        UUID secondId = UUID.randomUUID();
        ReflectionTestUtils.setField(second, "id", secondId);
        second.setUser(user);
        second.setDevice("Firefox");
        second.setIpAddress("10.0.0.2");
        second.setLastActive(Instant.now());

        when(refreshTokens.findByUserAndRevokedFalseOrderByCreatedAtDesc(user)).thenReturn(List.of(first, second));

        List<?> sessions = authService.getActiveSessions(user, firstId.toString());
        assertEquals(2, sessions.size());

        @SuppressWarnings("unchecked")
        List<id.ac.ui.cs.advprog.bidmart.backend.auth.dto.SessionResponseDTO> typed =
                (List<id.ac.ui.cs.advprog.bidmart.backend.auth.dto.SessionResponseDTO>) sessions;
        assertTrue(typed.get(0).current);
        assertFalse(typed.get(1).current);
    }

    @Test
    void changePasswordAndUpdateProfile() {
        ChangePasswordRequestDTO req = new ChangePasswordRequestDTO();
        req.currentPassword = "password123";
        req.newPassword = "newpassword123";

        authService.changePassword(user, req);
        verify(users).save(user);

        when(users.save(user)).thenReturn(user);
        User updated = authService.updateProfile(user, "Updated", "https://avatar");
        assertEquals("Updated", updated.getDisplayName());
        assertEquals("https://avatar", updated.getAvatarUrl());

        ChangePasswordRequestDTO wrong = new ChangePasswordRequestDTO();
        wrong.currentPassword = "bad";
        wrong.newPassword = "x";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.changePassword(user, wrong));
        assertEquals("Current password invalid", ex.getMessage());
    }

    @Test
    void adminUserAndRoleFlows() {
        UUID userId = UUID.randomUUID();
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(users.searchUsers("user", UserStatus.ACTIVE)).thenReturn(List.of(user));

        List<UserResponseDTO> listed = authService.adminListUsers("user", "BUYER", "active", 0, 20);
        assertEquals(1, listed.size());

        List<UserResponseDTO> listedCaseInsensitiveRole = authService.adminListUsers("user", "buyer", "active", 0, 20);
        assertEquals(1, listedCaseInsensitiveRole.size());

        UserResponseDTO one = authService.adminGetUser(userId);
        assertEquals("user@example.com", one.email);

        authService.adminUpdateUserStatus(userId, "SUSPENDED", "policy");
        verify(refreshTokens).revokeAllByUser(user);
        verify(eventPublisher).publishEvent(any(Object.class));

        authService.adminUpdateUserRoles(userId, List.of("ADMIN", "BUYER"));
        verify(eventPublisher, times(2)).publishEvent(any(Object.class));

        Role role = new Role();
        role.setName("ADMIN");
        role.setPermissions("users:write");
        when(roles.findAll()).thenReturn(List.of(role));
        assertEquals(1, authService.adminListRoles().size());

        RoleRequestDTO createReq = new RoleRequestDTO();
        createReq.name = "admin";
        createReq.permissions = List.of("users:write");
        when(roles.findByNameIgnoreCase("admin")).thenReturn(Optional.empty());
        authService.adminCreateRole(createReq);

        UUID roleId = UUID.randomUUID();
        when(roles.findById(roleId)).thenReturn(Optional.of(role));
        authService.adminUpdateRole(roleId, createReq);

        UUID missingRole = UUID.randomUUID();
        when(roles.findById(missingRole)).thenReturn(Optional.empty());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> authService.adminUpdateRole(missingRole, createReq));
        assertEquals("Role not found", ex.getMessage());
    }

    @Test
    void adminListUsers_HandlesBlankStatusAndRoleFilters() {
        User another = new User();
        another.setEmail("another@example.com");
        another.setDisplayName("Another");
        another.setEmailVerified(true);
        another.setStatus(UserStatus.ACTIVE);
        another.setRolesList(List.of("SELLER"));
        ReflectionTestUtils.setField(another, "createdAt", Instant.now().minusSeconds(10));

        user.setRolesList(List.of("BUYER"));
        ReflectionTestUtils.setField(user, "createdAt", Instant.now());

        when(users.searchUsers("", null)).thenReturn(List.of(user, another));

        List<UserResponseDTO> all = authService.adminListUsers("", null, "   ", 0, 10);
        assertEquals(2, all.size());

        List<UserResponseDTO> blankRole = authService.adminListUsers("", "   ", "", 0, 10);
        assertEquals(2, blankRole.size());

        List<UserResponseDTO> onlySeller = authService.adminListUsers("", "seller", "", 0, 10);
        assertEquals(1, onlySeller.size());
        assertEquals("another@example.com", onlySeller.get(0).email);

        List<UserResponseDTO> nullStatus = authService.adminListUsers("", "", null, 0, 10);
        assertEquals(2, nullStatus.size());
    }

    @Test
    void adminUpdateUserStatus_ActiveDoesNotPublishEventOrRevokeTokens() {
        UUID userId = UUID.randomUUID();
        when(users.findById(userId)).thenReturn(Optional.of(user));

        UserResponseDTO response = authService.adminUpdateUserStatus(userId, "ACTIVE", "-");

        assertEquals("user@example.com", response.email);
        verify(eventPublisher, never()).publishEvent(any(Object.class));
        verify(refreshTokens, never()).revokeAllByUser(any(User.class));
    }

    @Test
    void adminRoleCreate_DuplicateThrows() {
        RoleRequestDTO req = new RoleRequestDTO();
        req.name = "admin";
        req.permissions = List.of("users:write");

        when(roles.findByNameIgnoreCase("admin")).thenReturn(Optional.of(new Role()));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.adminCreateRole(req));
        assertEquals("Role already exists", ex.getMessage());
    }

    @Test
    void getUserHelpersAndRefreshWithDesign() {
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        assertEquals("user@example.com", authService.getUserByEmail(" USER@EXAMPLE.COM ").getEmail());

        UUID userId = UUID.randomUUID();
        when(users.findById(userId)).thenReturn(Optional.of(user));
        assertEquals("user@example.com", authService.getUserById(userId).getEmail());

        UUID missingId = UUID.randomUUID();
        when(users.findById(missingId)).thenReturn(Optional.empty());
        assertEquals("User not found", assertThrows(IllegalArgumentException.class,
            () -> authService.getUserById(missingId)).getMessage());

        when(users.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        assertEquals("User not found", assertThrows(IllegalArgumentException.class,
            () -> authService.getUserByEmail("missing@example.com")).getMessage());

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken("refresh");
        refreshToken.setRevoked(false);
        refreshToken.setExpiresAt(Instant.now().plusSeconds(60));
        when(refreshTokens.findByToken("refresh")).thenReturn(Optional.of(refreshToken));
        when(refreshTokens.save(refreshToken)).thenReturn(refreshToken);

        LoginSuccessResponseDTO refreshed = authService.refreshWithDesign("refresh");
        assertNotNull(refreshed.accessToken);

        refreshToken.setRevoked(true);
        IllegalArgumentException revoked = assertThrows(IllegalArgumentException.class,
                () -> authService.refreshWithDesign("refresh"));
        assertEquals("Refresh token revoked", revoked.getMessage());

        refreshToken.setRevoked(false);
        refreshToken.setExpiresAt(Instant.now().minusSeconds(60));
        IllegalArgumentException expired = assertThrows(IllegalArgumentException.class,
                () -> authService.refreshWithDesign("refresh"));
        assertEquals("Refresh token expired", expired.getMessage());
    }

    @Test
    void loginWithDesign_UsesDeviceAndRemoteIpFallbackPaths() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.email = "user@example.com";
        request.password = "password123";

        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(refreshTokens.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(servletRequest.getHeader("User-Agent")).thenReturn(" ");
        when(servletRequest.getHeader("X-Forwarded-For")).thenReturn(" ");
        when(servletRequest.getRemoteAddr()).thenReturn("127.0.0.9");

        Object res = authService.loginWithDesign(request, servletRequest);
        assertTrue(res instanceof LoginSuccessResponseDTO);
    }

    @Test
    void forgotPassword_UsesFrontendFallbackWhenBlank() {
        AppProperties blankAppProps = new AppProperties();
        blankAppProps.setFrontendUrl(" ");
        AuthProperties authProperties = new AuthProperties();
        authProperties.setSecret("my-super-secret-key-that-is-at-least-32-bytes");
        authProperties.setAccessTokenExpiration(3600000L);
        authProperties.setRefreshTokenExpiration(7200000L);

        AuthService service = new AuthService(
                users,
                refreshTokens,
                roles,
                verificationTokens,
                resetTokens,
                partialAuthSessions,
                authProperties,
                blankAppProps,
                emailService,
                totpService,
                eventPublisher
        );

        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        service.forgotPassword("user@example.com");

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendResetPasswordEmail(org.mockito.ArgumentMatchers.eq("user@example.com"), linkCaptor.capture());
        assertTrue(linkCaptor.getValue().startsWith("http://localhost:3000/auth/reset?token="));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void forgotPassword_UsesConfiguredFrontendUrlWhenPresent() {
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        authService.forgotPassword("user@example.com");

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendResetPasswordEmail(org.mockito.ArgumentMatchers.eq("user@example.com"), linkCaptor.capture());
        assertTrue(linkCaptor.getValue().startsWith("http://frontend.local/auth/reset?token="));
    }

    @Test
    void forgotPassword_UsesFrontendFallbackWhenNull() {
        AppProperties nullFrontend = new AppProperties();
        nullFrontend.setFrontendUrl(null);
        AuthProperties authProperties = new AuthProperties();
        authProperties.setSecret("my-super-secret-key-that-is-at-least-32-bytes");
        authProperties.setAccessTokenExpiration(3600000L);
        authProperties.setRefreshTokenExpiration(7200000L);

        AuthService service = new AuthService(
                users,
                refreshTokens,
                roles,
                verificationTokens,
                resetTokens,
                partialAuthSessions,
                authProperties,
                nullFrontend,
                emailService,
                totpService,
                eventPublisher
        );

        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        service.forgotPassword("user@example.com");

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendResetPasswordEmail(org.mockito.ArgumentMatchers.eq("user@example.com"), linkCaptor.capture());
        assertTrue(linkCaptor.getValue().startsWith("http://localhost:3000/auth/reset?token="));
    }

    @Test
    void extractDeviceAndExtractIp_PrivateBranches() {
        assertEquals("Unknown device", ReflectionTestUtils.invokeMethod(authService, "extractDevice", (HttpServletRequest) null));
        assertEquals("unknown", ReflectionTestUtils.invokeMethod(authService, "extractIp", (HttpServletRequest) null));

        HttpServletRequest noUa = org.mockito.Mockito.mock(HttpServletRequest.class);
        when(noUa.getHeader("User-Agent")).thenReturn(null);
        assertEquals("Unknown device", ReflectionTestUtils.invokeMethod(authService, "extractDevice", noUa));

        HttpServletRequest longUa = org.mockito.Mockito.mock(HttpServletRequest.class);
        String ua = "X".repeat(200);
        when(longUa.getHeader("User-Agent")).thenReturn(ua);
        String extracted = ReflectionTestUtils.invokeMethod(authService, "extractDevice", longUa);
        assertEquals(180, extracted.length());

        HttpServletRequest noForwarded = org.mockito.Mockito.mock(HttpServletRequest.class);
        when(noForwarded.getHeader("X-Forwarded-For")).thenReturn(null);
        when(noForwarded.getRemoteAddr()).thenReturn("127.0.0.1");
        assertEquals("127.0.0.1", ReflectionTestUtils.invokeMethod(authService, "extractIp", noForwarded));
    }

    @Test
    void adminListRoles_ToRoleResponseHandlesBlankAndNullPermissions() {
        Role blankPermissions = new Role();
        blankPermissions.setName("BLANK");
        blankPermissions.setPermissions("   ");

        Role nullPermissions = new Role();
        nullPermissions.setName("NULL");
        nullPermissions.setPermissions(null);

        when(roles.findAll()).thenReturn(new ArrayList<>(List.of(blankPermissions, nullPermissions)));

        var listed = authService.adminListRoles();
        assertEquals(2, listed.size());
        assertEquals(List.of(), listed.get(0).permissions);
        assertEquals(List.of(), listed.get(1).permissions);
    }
}
