package id.ac.ui.cs.advprog.bidmart.backend.auth.security;

import id.ac.ui.cs.advprog.bidmart.backend.auth.config.AuthProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
        String token = jwtService.generateAccessToken(123L, "test@test.com");
        assertNotNull(token);
        assertTrue(jwtService.isValid(token));

        Claims claims = jwtService.parseClaims(token);
        assertEquals("123", claims.getSubject());
        assertEquals("test@test.com", claims.get("email"));
    }

    @Test
    void testInvalidToken() {
        assertFalse(jwtService.isValid("invalid-token"));
    }
}
