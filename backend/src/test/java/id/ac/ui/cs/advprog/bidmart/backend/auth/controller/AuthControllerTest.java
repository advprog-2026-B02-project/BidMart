package id.ac.ui.cs.advprog.bidmart.backend.auth.controller;

import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.AuthResponse;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.LoginRequest;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.RefreshRequest;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.RegisterRequest;
import id.ac.ui.cs.advprog.bidmart.backend.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

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
    void verify_email() {
        doNothing().when(authService).verifyEmail("token");

        ResponseEntity<String> res = authController.verify("token");
        assertEquals(200, res.getStatusCode().value());
        assertNotNull(res.getBody());
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
    void logout() {
        RefreshRequest req = new RefreshRequest();
        req.refreshToken = "token";

        doNothing().when(authService).logout("token");

        ResponseEntity<Void> res = authController.logout(req);
        assertEquals(200, res.getStatusCode().value());
        verify(authService).logout("token");
    }
}
