package id.ac.ui.cs.advprog.bidmart.backend.auth.controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.AuthResponse;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.LoginRequest;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.LoginRequestDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.LoginSuccessResponseDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.RefreshRequest;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.RegisterRequest;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.RegisterRequestDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.TwoFactorConfirmRequestDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.TwoFactorDisableRequestDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.TwoFactorSetupRequestDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.TwoFactorSetupResponseDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.TwoFactorVerifyRequestDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.UserResponseDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.VerifyEmailRequestDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.entity.User;
import id.ac.ui.cs.advprog.bidmart.backend.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Mock
    private HttpServletRequest servletRequest;

    @Test
    void register() {
        RegisterRequest req = new RegisterRequest();
        req.email = "test@example.com";
        req.password = "pass";
        req.displayName = "Test";

        doNothing().when(authService).register(req.email, req.password, req.displayName);

        ResponseEntity<Void> res = authController.register(req);
        assertEquals(200, res.getStatusCode().value());
        verify(authService).register("test@example.com", "pass", "Test");
    }

    @Test
    void registerV2() {
        RegisterRequestDTO req = new RegisterRequestDTO();
        req.email = "test@example.com";
        req.password = "password123";
        req.displayName = "Test";

        UserResponseDTO user = new UserResponseDTO(UUID.randomUUID(), req.email, req.displayName, false, Instant.now(), List.of("BUYER"));
        when(authService.registerAndReturn(req)).thenReturn(user);

        ResponseEntity<UserResponseDTO> res = authController.register(req);

        assertEquals(201, res.getStatusCode().value());
        assertEquals("test@example.com", res.getBody().email);
    }

    @Test
    void verify_email() {
        doNothing().when(authService).verifyEmail("token");

        ResponseEntity<String> res = authController.verify("token");
        assertEquals(200, res.getStatusCode().value());
        assertNotNull(res.getBody());
        verify(authService).verifyEmail("token");
    }

    @Test
    void verifyEmailBody() {
        VerifyEmailRequestDTO req = new VerifyEmailRequestDTO();
        req.token = "token";

        ResponseEntity<Map<String, String>> res = authController.verifyEmail(req);

        assertEquals(200, res.getStatusCode().value());
        assertEquals("Email verified", res.getBody().get("message"));
        verify(authService).verifyEmail("token");
    }

    @Test
    void login() {
        LoginRequest req = new LoginRequest();
        req.email = "test@example.com";
        req.password = "pass";

        AuthResponse authRes = new AuthResponse("access", "refresh");
        when(authService.login(req.email, req.password)).thenReturn(authRes);

        ResponseEntity<AuthResponse> res = authController.login(req);
        assertEquals(200, res.getStatusCode().value());
        assertEquals("access", res.getBody().accessToken);
        verify(authService).login("test@example.com", "pass");
    }

    @Test
    void loginV2() {
        LoginRequestDTO req = new LoginRequestDTO();
        req.email = "test@example.com";
        req.password = "pass";

        AuthResponse loginResponse = new AuthResponse("access", "refresh");
        when(authService.loginWithDesign(req, servletRequest)).thenReturn(loginResponse);

        ResponseEntity<?> res = authController.login(req, servletRequest);
        assertEquals(200, res.getStatusCode().value());
        verify(authService).loginWithDesign(req, servletRequest);
    }

    @Test
    void refresh() {
        RefreshRequest req = new RefreshRequest();
        req.refreshToken = "token";

        AuthResponse authRes = new AuthResponse("access", "refresh");
        when(authService.refresh("token")).thenReturn(authRes);

        ResponseEntity<AuthResponse> res = authController.refresh(req);
        assertEquals(200, res.getStatusCode().value());
        assertEquals("access", res.getBody().accessToken);
        verify(authService).refresh("token");
    }

    @Test
    void refreshV2() {
        RefreshRequest req = new RefreshRequest();
        req.refreshToken = "token";

        LoginSuccessResponseDTO loginSuccess = new LoginSuccessResponseDTO("access", "token", 3600L,
                new UserResponseDTO(UUID.randomUUID(), "e@x.com", "Name", true, Instant.now(), List.of("BUYER")));
        when(authService.refreshWithDesign("token")).thenReturn(loginSuccess);

        ResponseEntity<?> res = authController.refreshV2(req);
        assertEquals(200, res.getStatusCode().value());
        verify(authService).refreshWithDesign("token");
    }

    @Test
    void logout() {
        RefreshRequest req = new RefreshRequest();
        req.refreshToken = "token";

        doNothing().when(authService).logout("token");

        ResponseEntity<Void> res = authController.logout(req);
        assertEquals(200, res.getStatusCode().value());
        verify(authService).logout("token");
    }

    @Test
    void logout_NullBody() {
        ResponseEntity<Void> res = authController.logout(null);
        assertEquals(200, res.getStatusCode().value());
        verify(authService, never()).logout(anyString());
    }

    @Test
    void logout_BlankToken() {
        RefreshRequest req = new RefreshRequest();
        req.refreshToken = "   ";

        ResponseEntity<Void> res = authController.logout(req);
        assertEquals(200, res.getStatusCode().value());
        verify(authService, never()).logout(anyString());
    }

    @Test
    void logout_NullToken() {
        RefreshRequest req = new RefreshRequest();
        req.refreshToken = null;

        ResponseEntity<Void> res = authController.logout(req);
        assertEquals(200, res.getStatusCode().value());
        verify(authService, never()).logout(anyString());
    }

    @Test
    void twoFactorEndpoints() {
        Authentication auth = mock(Authentication.class);
        User user = new User();
        user.setEmail("test@example.com");

        when(auth.getPrincipal()).thenReturn(Map.of("email", "test@example.com"));
        when(authService.getUserByEmail("test@example.com")).thenReturn(user);

        TwoFactorSetupRequestDTO setupRequest = new TwoFactorSetupRequestDTO();
        setupRequest.method = "TOTP";
        when(authService.setupTwoFactor(user, "TOTP")).thenReturn(new TwoFactorSetupResponseDTO("secret", null, List.of("CODE1234")));

        ResponseEntity<?> setupRes = authController.setupTwoFactor(setupRequest, auth);
        assertEquals(200, setupRes.getStatusCode().value());

        TwoFactorConfirmRequestDTO confirmRequest = new TwoFactorConfirmRequestDTO();
        confirmRequest.code = "123456";
        ResponseEntity<Map<String, String>> confirmRes = authController.confirmTwoFactor(confirmRequest, auth);
        assertEquals("2FA enabled", confirmRes.getBody().get("message"));

        TwoFactorDisableRequestDTO disableRequest = new TwoFactorDisableRequestDTO();
        disableRequest.password = "password123";
        ResponseEntity<Map<String, String>> disableRes = authController.disableTwoFactor(disableRequest, auth);
        assertEquals("2FA disabled", disableRes.getBody().get("message"));

        TwoFactorVerifyRequestDTO verifyRequest = new TwoFactorVerifyRequestDTO();
        verifyRequest.partialToken = "p";
        verifyRequest.method = "TOTP";
        verifyRequest.code = "123456";
        when(authService.verifyTwoFactor(verifyRequest, servletRequest)).thenReturn(
            new LoginSuccessResponseDTO("a", "r", 60L,
                new UserResponseDTO(UUID.randomUUID(), "e@x.com", "Name", true, Instant.now(), List.of("BUYER"))));
        ResponseEntity<?> verifyRes = authController.verifyTwoFactor(verifyRequest, servletRequest);
        assertEquals(200, verifyRes.getStatusCode().value());
    }

    @Test
    void twoFactor_UnauthorizedPrincipal() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(Map.of("name", "missing-email"));

        TwoFactorSetupRequestDTO setupRequest = new TwoFactorSetupRequestDTO();
        setupRequest.method = "TOTP";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authController.setupTwoFactor(setupRequest, auth));
        assertEquals("Unauthorized", ex.getMessage());
    }

    @Test
    void twoFactor_UnauthorizedNullAuthentication() {
        TwoFactorSetupRequestDTO setupRequest = new TwoFactorSetupRequestDTO();
        setupRequest.method = "TOTP";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authController.setupTwoFactor(setupRequest, null));
        assertEquals("Unauthorized", ex.getMessage());
    }

    @Test
    void twoFactor_UnauthorizedNullPrincipal() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(null);

        TwoFactorSetupRequestDTO setupRequest = new TwoFactorSetupRequestDTO();
        setupRequest.method = "TOTP";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authController.setupTwoFactor(setupRequest, auth));
        assertEquals("Unauthorized", ex.getMessage());
    }
}
