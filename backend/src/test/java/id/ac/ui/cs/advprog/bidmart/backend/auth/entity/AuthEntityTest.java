package id.ac.ui.cs.advprog.bidmart.backend.auth.entity;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuthEntityTest {

    @Test
    void testEmailVerificationToken() {
        EmailVerificationToken t = new EmailVerificationToken();
        t.setToken("token");
        
        User user = new User();
        t.setUser(user);
        
        Instant now = Instant.now();
        t.setExpiresAt(now);
        t.setUsedAt(now);

        assertEquals("token", t.getToken());
        assertNotNull(t.getUser());
        assertEquals(now, t.getExpiresAt());
        assertEquals(now, t.getUsedAt());
        assertNotNull(t.getCreatedAt());
    }

    @Test
    void testPasswordResetToken() {
        PasswordResetToken t = new PasswordResetToken();
        t.setToken("token");
        
        User user = new User();
        t.setUser(user);
        
        Instant now = Instant.now();
        t.setExpiresAt(now);
        t.setUsedAt(now);

        assertEquals("token", t.getToken());
        assertNotNull(t.getUser());
        assertEquals(now, t.getExpiresAt());
    }

    @Test
    void testRefreshToken() {
        RefreshToken t = new RefreshToken();
        t.setToken("token");
        
        User user = new User();
        t.setUser(user);
        
        Instant now = Instant.now().plusSeconds(100);
        t.setExpiresAt(now);

        assertEquals("token", t.getToken());
        assertNotNull(t.getUser());
        assertEquals(now, t.getExpiresAt());
        assertNotNull(t.getCreatedAt());
    }

    @Test
    void testUser() {
        User u = new User();
        u.setEmail("e");
        u.setPasswordHash("p");
        u.setDisplayName("d");
        u.setAvatarUrl("a");
        u.setEmailVerified(true);
        u.preUpdate();

        assertEquals("e", u.getEmail());
        assertEquals("p", u.getPasswordHash());
        assertEquals("d", u.getDisplayName());
        assertEquals("a", u.getAvatarUrl());
        assertTrue(u.isEmailVerified());
        assertNotNull(u.getCreatedAt());
        assertNotNull(u.getUpdatedAt());
    }
}
