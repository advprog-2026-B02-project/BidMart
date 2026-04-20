package id.ac.ui.cs.advprog.bidmart.backend.auth.controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.SessionResponseDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.entity.User;
import id.ac.ui.cs.advprog.bidmart.backend.auth.service.AuthService;

@ExtendWith(MockitoExtension.class)
class SessionControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private SessionController sessionController;

    @Test
    void me_NoAuth() {
        ResponseEntity<Map<String, Object>> res = sessionController.me(null);
        assertEquals(401, res.getStatusCode().value());
    }

    @Test
    void me_NullPrincipal() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(null);

        ResponseEntity<Map<String, Object>> res = sessionController.me(auth);
        assertEquals(401, res.getStatusCode().value());
    }

    @Test
    void me_Success() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(Map.of("userId", "1", "email", "t@t.com"));

        ResponseEntity<Map<String, Object>> res = sessionController.me(auth);
        assertEquals(200, res.getStatusCode().value());
        assertEquals("1", res.getBody().get("userId"));
    }

    @Test
    void listSessions() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(Map.of("email", "t@t.com", "sessionId", "abc"));

        User user = new User();
        user.setEmail("t@t.com");
        when(authService.getUserByEmail("t@t.com")).thenReturn(user);

        SessionResponseDTO session = new SessionResponseDTO(UUID.randomUUID(), "Chrome", "127.0.0.1", Instant.now(), true);
        when(authService.getActiveSessions(user, "abc")).thenReturn(List.of(session));

        ResponseEntity<List<SessionResponseDTO>> response = sessionController.listSessions(auth);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        verify(authService).getActiveSessions(user, "abc");
    }

    @Test
    void listSessions_Unauthorized() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(Map.of("name", "no-email"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> sessionController.listSessions(auth));
        assertEquals("Unauthorized", ex.getMessage());
    }

    @Test
    void listSessions_UnauthorizedNullAuthentication() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> sessionController.listSessions(null));
        assertEquals("Unauthorized", ex.getMessage());
    }

    @Test
    void listSessions_UnauthorizedNullPrincipal() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> sessionController.listSessions(auth));
        assertEquals("Unauthorized", ex.getMessage());
    }

    @Test
    void revokeSession() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(Map.of("email", "t@t.com"));

        User user = new User();
        user.setEmail("t@t.com");
        when(authService.getUserByEmail("t@t.com")).thenReturn(user);

        UUID sessionId = UUID.randomUUID();
        ResponseEntity<Void> response = sessionController.revokeSession(sessionId, auth);

        assertEquals(204, response.getStatusCode().value());
        verify(authService).revokeSession(user, sessionId);
    }

    @Test
    void listSessions_CurrentSessionMissing() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(Map.of("email", "t@t.com"));

        User user = new User();
        user.setEmail("t@t.com");
        when(authService.getUserByEmail("t@t.com")).thenReturn(user);

        when(authService.getActiveSessions(user, null)).thenReturn(List.of());

        ResponseEntity<List<SessionResponseDTO>> response = sessionController.listSessions(auth);
        assertEquals(200, response.getStatusCode().value());
        verify(authService).getActiveSessions(user, null);
    }
}
