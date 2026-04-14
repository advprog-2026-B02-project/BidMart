package id.ac.ui.cs.advprog.bidmart.backend.auth.security;

import id.ac.ui.cs.advprog.bidmart.backend.auth.config.AuthProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    private JwtAuthFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        AuthProperties props = new AuthProperties();
        props.setSecret("my-super-secret-key-that-is-at-least-32-bytes");
        props.setAccessTokenExpiration(3600000L);
        filter = new JwtAuthFilter(props);
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNotFilter() {
        when(request.getRequestURI()).thenReturn("/auth/login");
        assertTrue(filter.shouldNotFilter(request));

        when(request.getRequestURI()).thenReturn("/api/orders");
        assertFalse(filter.shouldNotFilter(request));
    }

    @Test
    void doFilterInternal_NoHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_InvalidHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic abc");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_InvalidJwt() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ValidJwt() throws Exception {
        AuthProperties props = new AuthProperties();
        props.setSecret("my-super-secret-key-that-is-at-least-32-bytes");
        props.setAccessTokenExpiration(3600000L);
        JwtService svc = new JwtService(props);
        String token = svc.generateAccessToken(1L, "email@email.com");

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertTrue(SecurityContextHolder.getContext().getAuthentication() != null);
    }
}
