package id.ac.ui.cs.advprog.bidmart.backend.auth.security;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import id.ac.ui.cs.advprog.bidmart.backend.auth.config.AuthProperties;
import io.jsonwebtoken.Claims;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        AuthProperties props = new AuthProperties();
        props.setSecret("my-super-secret-key-that-is-at-least-32-bytes");
        props.setAccessTokenExpiration(360000L);
        jwtService = new JwtService(props);
    }

    @Test
    void testGenerateAndValidate() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId, "test@test.com");
        assertNotNull(token);
        assertTrue(jwtService.isValid(token));

        Claims claims = jwtService.parseClaims(token);
        assertEquals(userId.toString(), claims.getSubject());
        assertEquals("test@test.com", claims.get("email"));
    }

    @Test
    void testInvalidToken() {
        assertFalse(jwtService.isValid("invalid-token"));
    }
}
