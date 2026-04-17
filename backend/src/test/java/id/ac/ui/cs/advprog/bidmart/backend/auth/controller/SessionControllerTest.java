package id.ac.ui.cs.advprog.bidmart.backend.auth.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionControllerTest {

    @InjectMocks
    private SessionController sessionController;

    @Test
    void me_NoAuth() {
        ResponseEntity<Map<String, Object>> res = sessionController.me(null);
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
}
