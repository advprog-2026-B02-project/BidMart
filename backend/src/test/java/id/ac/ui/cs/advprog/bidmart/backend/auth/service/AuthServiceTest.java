package id.ac.ui.cs.advprog.bidmart.backend.auth.service;

import id.ac.ui.cs.advprog.bidmart.backend.auth.config.AppProperties;
import id.ac.ui.cs.advprog.bidmart.backend.auth.config.AuthProperties;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.AuthResponse;
import id.ac.ui.cs.advprog.bidmart.backend.auth.entity.EmailVerificationToken;
import id.ac.ui.cs.advprog.bidmart.backend.auth.entity.PasswordResetToken;
import id.ac.ui.cs.advprog.bidmart.backend.auth.entity.RefreshToken;
import id.ac.ui.cs.advprog.bidmart.backend.auth.entity.User;
import id.ac.ui.cs.advprog.bidmart.backend.auth.repository.EmailVerificationTokenRepository;
import id.ac.ui.cs.advprog.bidmart.backend.auth.repository.PasswordResetTokenRepository;
import id.ac.ui.cs.advprog.bidmart.backend.auth.repository.RefreshTokenRepository;
import id.ac.ui.cs.advprog.bidmart.backend.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository users;
    @Mock
    private RefreshTokenRepository refreshTokens;
    @Mock
    private EmailVerificationTokenRepository verificationTokens;
    @Mock
    private PasswordResetTokenRepository resetTokens;
    @Mock
    private EmailService emailService;

    private AuthService authService;

    private User user;
    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        AuthProperties authProps = new AuthProperties();
        authProps.setSecret("my-super-secret-key-that-is-at-least-32-bytes");
        authProps.setAccessTokenExpiration(3600000L);
        authProps.setRefreshTokenExpiration(7200000L);

        AppProperties appProps = new AppProperties();
        appProps.setBaseUrl("http://localhost:8080");

        authService = new AuthService(
                users, refreshTokens, verificationTokens, resetTokens,
                authProps, appProps, emailService
        );

        user = new User();
        user.setEmail("test@example.com");
        user.setPasswordHash(encoder.encode("password123"));
        user.setDisplayName("Test User");
        user.setEmailVerified(true);
    }

    @Test
    void register_NewUser() {
        when(users.findByEmail("new@example.com")).thenReturn(Optional.empty());

        authService.register("new@example.com", "password", "New User");

        verify(users).save(any(User.class));
        verify(verificationTokens).save(any(EmailVerificationToken.class));
        verify(emailService).sendVerificationEmail(eq("new@example.com"), anyString());
    }

    @Test
    void register_ExistingUnverifiedUser() {
        user.setEmailVerified(false);
        user.setEmail("exist@example.com");
        when(users.findByEmail("exist@example.com")).thenReturn(Optional.of(user));

        authService.register("exist@example.com", "password", "User");

        verify(users).save(user);
        verify(verificationTokens).deleteByUserAndUsedAtIsNull(user);
        verify(verificationTokens).save(any(EmailVerificationToken.class));
    }

    @Test
    void register_ExistingVerifiedUser_Throws() {
        when(users.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, () ->
            authService.register("test@example.com", "pass", "Name")
        );
    }

    @Test
    void login_Success() {
        when(users.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        
        AuthResponse res = authService.login("test@example.com", "password123");
        
        assertNotNull(res.accessToken);
        assertNotNull(res.refreshToken);
        verify(refreshTokens).save(any(RefreshToken.class));
    }

    @Test
    void login_InvalidPassword() {
        when(users.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        
        assertThrows(IllegalArgumentException.class, () ->
            authService.login("test@example.com", "wrongpass")
        );
    }

    @Test
    void login_UnverifiedEmail() {
        user.setEmailVerified(false);
        when(users.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        
        assertThrows(IllegalStateException.class, () ->
            authService.login("test@example.com", "password123")
        );
    }

    @Test
    void verifyEmail_Success() {
        EmailVerificationToken t = new EmailVerificationToken();
        t.setUser(user);
        t.setExpiresAt(Instant.now().plusSeconds(100));
        when(verificationTokens.findByToken("token")).thenReturn(Optional.of(t));

        authService.verifyEmail("token");

        assertTrue(user.isEmailVerified());
        assertNotNull(t.getUsedAt());
        verify(users).save(user);
        verify(verificationTokens).save(t);
    }

    @Test
    void verifyEmail_AlreadyUsed() {
        EmailVerificationToken t = new EmailVerificationToken();
        t.setUsedAt(Instant.now());
        when(verificationTokens.findByToken("token")).thenReturn(Optional.of(t));

        assertThrows(IllegalArgumentException.class, () -> authService.verifyEmail("token"));
    }

    @Test
    void verifyEmail_Expired() {
        EmailVerificationToken t = new EmailVerificationToken();
        t.setExpiresAt(Instant.now().minusSeconds(100));
        when(verificationTokens.findByToken("token")).thenReturn(Optional.of(t));

        assertThrows(IllegalArgumentException.class, () -> authService.verifyEmail("token"));
    }

    @Test
    void verifyEmail_InvalidToken() {
        when(verificationTokens.findByToken("invalid")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> authService.verifyEmail("invalid"));
    }

    @Test
    void refresh_Success() {
        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setToken("oldrefresh");
        rt.setExpiresAt(Instant.now().plusSeconds(1000));
        when(refreshTokens.findByToken("oldrefresh")).thenReturn(Optional.of(rt));

        AuthResponse res = authService.refresh("oldrefresh");
        assertNotNull(res.accessToken);
        assertEquals("oldrefresh", res.refreshToken);
    }

    @Test
    void refresh_Revoked() {
        RefreshToken rt = new RefreshToken();
        rt.setRevoked(true);
        when(refreshTokens.findByToken("oldrefresh")).thenReturn(Optional.of(rt));

        assertThrows(IllegalArgumentException.class, () -> authService.refresh("oldrefresh"));
    }

    @Test
    void refresh_Expired() {
        RefreshToken rt = new RefreshToken();
        rt.setExpiresAt(Instant.now().minusSeconds(100));
        rt.setRevoked(false);
        when(refreshTokens.findByToken("oldrefresh")).thenReturn(Optional.of(rt));

        assertThrows(IllegalArgumentException.class, () -> authService.refresh("oldrefresh"));
    }

    @Test
    void refresh_InvalidToken() {
        when(refreshTokens.findByToken("invalid")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> authService.refresh("invalid"));
    }

    @Test
    void logout_Success() {
        RefreshToken rt = new RefreshToken();
        when(refreshTokens.findByToken("token")).thenReturn(Optional.of(rt));

        authService.logout("token");

        assertTrue(rt.isRevoked());
        verify(refreshTokens).save(rt);
    }

    @Test
    void logout_InvalidToken() {
        when(refreshTokens.findByToken("invalid")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> authService.logout("invalid"));
    }

    @Test
    void forgotPassword_Success() {
        when(users.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        authService.forgotPassword("test@example.com");

        verify(resetTokens).deleteByUserAndUsedAtIsNull(user);
        verify(resetTokens).save(any(PasswordResetToken.class));
        verify(emailService).sendResetPasswordEmail(eq("test@example.com"), anyString());
    }

    @Test
    void forgotPassword_UserNotFound() {
        when(users.findByEmail("notfound@example.com")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> authService.forgotPassword("notfound@example.com"));
    }

    @Test
    void resetPassword_Success() {
        PasswordResetToken t = new PasswordResetToken();
        t.setUser(user);
        t.setExpiresAt(Instant.now().plusSeconds(100));
        when(resetTokens.findByToken("token")).thenReturn(Optional.of(t));

        authService.resetPassword("token", "newpass");

        assertTrue(encoder.matches("newpass", user.getPasswordHash()));
        assertNotNull(t.getUsedAt());
        verify(users).save(user);
        verify(resetTokens).save(t);
    }

    @Test
    void resetPassword_InvalidToken() {
        when(resetTokens.findByToken("invalid")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> authService.resetPassword("invalid", "p"));
    }

    @Test
    void resetPassword_AlreadyUsed() {
        PasswordResetToken t = new PasswordResetToken();
        t.setUsedAt(Instant.now());
        when(resetTokens.findByToken("token")).thenReturn(Optional.of(t));
        assertThrows(IllegalArgumentException.class, () -> authService.resetPassword("token", "p"));
    }

    @Test
    void resetPassword_Expired() {
        PasswordResetToken t = new PasswordResetToken();
        t.setExpiresAt(Instant.now().minusSeconds(100));
        when(resetTokens.findByToken("token")).thenReturn(Optional.of(t));
        assertThrows(IllegalArgumentException.class, () -> authService.resetPassword("token", "p"));
    }

    @Test
    void validateResetToken_Success() {
        PasswordResetToken t = new PasswordResetToken();
        t.setExpiresAt(Instant.now().plusSeconds(100));
        when(resetTokens.findByToken("token")).thenReturn(Optional.of(t));
        assertDoesNotThrow(() -> authService.validateResetToken("token"));
    }

    @Test
    void validateResetToken_InvalidToken() {
        when(resetTokens.findByToken("invalid")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> authService.validateResetToken("invalid"));
    }

    @Test
    void validateResetToken_AlreadyUsed() {
        PasswordResetToken t = new PasswordResetToken();
        t.setUsedAt(Instant.now());
        when(resetTokens.findByToken("token")).thenReturn(Optional.of(t));
        assertThrows(IllegalArgumentException.class, () -> authService.validateResetToken("token"));
    }

    @Test
    void validateResetToken_Expired() {
        PasswordResetToken t = new PasswordResetToken();
        t.setExpiresAt(Instant.now().minusSeconds(100));
        when(resetTokens.findByToken("token")).thenReturn(Optional.of(t));
        assertThrows(IllegalArgumentException.class, () -> authService.validateResetToken("token"));
    }
}
