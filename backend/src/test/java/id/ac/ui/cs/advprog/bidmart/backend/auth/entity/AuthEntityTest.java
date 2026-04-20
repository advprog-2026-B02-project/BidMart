package id.ac.ui.cs.advprog.bidmart.backend.auth.entity;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuthEntityTest {

    @Test
    void testEmailVerificationToken() {
        EmailVerificationToken t = new EmailVerificationToken();
        ReflectionTestUtils.setField(t, "id", UUID.randomUUID());
        t.setToken("token");
        
        User user = new User();
        t.setUser(user);
        
        Instant now = Instant.now();
        t.setExpiresAt(now);
        t.setUsedAt(now);

        assertEquals("token", t.getToken());
        assertNotNull(t.getId());
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

        org.springframework.test.util.ReflectionTestUtils.setField(u, "roles", "   ");
        assertTrue(u.getRolesList().isEmpty());

        org.springframework.test.util.ReflectionTestUtils.setField(u, "roles", null);
        assertTrue(u.getRolesList().isEmpty());
    }

    @Test
    void testPartialAuthSessionAndRoleAndStatus() {
        PartialAuthSession partial = new PartialAuthSession();
        ReflectionTestUtils.setField(partial, "id", UUID.randomUUID());
        User user = new User();
        partial.setUser(user);
        partial.setPartialToken("pt");
        partial.setMethods("TOTP");
        Instant now = Instant.now();
        partial.setExpiresAt(now);
        partial.setUsed(true);
        partial.setEmailOtpHash("hash");
        partial.setEmailOtpExpiresAt(now.plusSeconds(60));

        assertNotNull(partial.getId());
        assertEquals("pt", partial.getPartialToken());
        assertEquals("TOTP", partial.getMethods());
        assertTrue(partial.isUsed());
        assertEquals("hash", partial.getEmailOtpHash());
        assertNotNull(partial.getEmailOtpExpiresAt());
        assertNotNull(partial.getUser());

        Role role = new Role();
        role.setName("ADMIN");
        role.setPermissions("users:write");
        assertEquals("ADMIN", role.getName());
        assertEquals("users:write", role.getPermissions());
        assertNotNull(role.getCreatedAt());

        assertEquals("ACTIVE", UserStatus.ACTIVE.name());
        assertEquals("SUSPENDED", UserStatus.SUSPENDED.name());

        User u = new User();
        u.setRolesList(java.util.List.of("BUYER"));
        assertEquals("BUYER", u.getRoles());
    }

    @Test
    void testUserRoleParsingAndNormalizationWithBlanks() {
        User u = new User();

        ReflectionTestUtils.setField(u, "roles", " buyer,   ,SELLER ");
        assertEquals(List.of("BUYER", "SELLER"), u.getRolesList());

        u.setRolesList(List.of(" buyer ", "", "SELLER", "seller"));
        assertEquals("BUYER,SELLER", u.getRoles());
    }
}
