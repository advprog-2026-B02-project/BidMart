package id.ac.ui.cs.advprog.bidmart.backend.auth.controller;

import java.time.Instant;
import java.util.Collections;
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

import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.ChangePasswordRequestDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.UpdateProfileRequest;
import id.ac.ui.cs.advprog.bidmart.backend.auth.entity.User;
import id.ac.ui.cs.advprog.bidmart.backend.auth.service.AuthService;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class MeControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private MeController meController;

    @Test
    void me_NoAuth() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> meController.me(null));
        assertEquals("Unauthorized", ex.getMessage());
    }

    @Test
    void me_Unauthenticated() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> meController.me(auth));
        assertEquals("Unauthorized", ex.getMessage());
    }

    @Test
    void me_NotFound() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(Map.of("email", "e"));
        when(authService.getUserByEmail("e")).thenThrow(new IllegalArgumentException("User not found"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> meController.me(auth));
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void me_Success() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(Map.of("email", "e"));
        User user = mock(User.class);
        when(user.getId()).thenReturn(UUID.randomUUID());
        when(user.getEmail()).thenReturn("e");
        when(user.getDisplayName()).thenReturn("D");
        when(user.getAvatarUrl()).thenReturn("A");
        when(user.getRolesList()).thenReturn(Collections.singletonList("BUYER"));
        when(user.isEmailVerified()).thenReturn(true);
        when(user.getStatus()).thenReturn(id.ac.ui.cs.advprog.bidmart.backend.auth.entity.UserStatus.ACTIVE);
        when(user.getCreatedAt()).thenReturn(Instant.now());
        when(authService.getUserByEmail("e")).thenReturn(user);

        ResponseEntity<?> res = meController.me(auth);
        assertEquals(200, res.getStatusCode().value());
    }

    @Test
    void me_Success_DefaultValues() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(Map.of("email", "e"));
        User user = mock(User.class);
        when(user.getId()).thenReturn(UUID.randomUUID());
        when(user.getEmail()).thenReturn("e");
        when(user.getDisplayName()).thenReturn(null);
        when(user.getAvatarUrl()).thenReturn(null);
        when(user.getRolesList()).thenReturn(Collections.singletonList("BUYER"));
        when(user.isEmailVerified()).thenReturn(true);
        when(user.getStatus()).thenReturn(id.ac.ui.cs.advprog.bidmart.backend.auth.entity.UserStatus.ACTIVE);
        when(user.getCreatedAt()).thenReturn(Instant.now());
        when(authService.getUserByEmail("e")).thenReturn(user);

        ResponseEntity<?> res = meController.me(auth);
        assertEquals(200, res.getStatusCode().value());
        Map<String, Object> body = (Map<String, Object>) res.getBody();
        assertEquals("Pengguna Baru", body.get("displayName"));
        assertEquals("", body.get("avatarUrl"));
    }

    @Test
    void updateProfile_Success() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(Map.of("email", "e"));
        User user = mock(User.class);
        when(user.getDisplayName()).thenReturn("N");
        when(user.getAvatarUrl()).thenReturn("U");
        when(authService.getUserByEmail("e")).thenReturn(user);
        when(authService.updateProfile(user, "N", "U")).thenReturn(user);

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.displayName = "N";
        req.avatarUrl = "U";

        ResponseEntity<?> res = meController.updateProfile(auth, req);
        assertEquals(200, res.getStatusCode().value());
        verify(authService).updateProfile(user, "N", "U");
    }

    @Test
    void updateProfile_NoAuth() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> meController.updateProfile(null, new UpdateProfileRequest()));
        assertEquals("Unauthorized", ex.getMessage());
    }

    @Test
    void updateProfile_NotFound() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(Map.of("email", "e"));
        when(authService.getUserByEmail("e")).thenThrow(new IllegalArgumentException("User not found"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> meController.updateProfile(auth, new UpdateProfileRequest()));
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void updateProfile_AvatarUrlNull() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(Map.of("email", "e"));
        User user = mock(User.class);
        when(user.getDisplayName()).thenReturn("N");
        when(user.getAvatarUrl()).thenReturn(null);
        when(authService.getUserByEmail("e")).thenReturn(user);
        when(authService.updateProfile(user, "N", null)).thenReturn(user);

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.displayName = "N";
        req.avatarUrl = null;

        ResponseEntity<?> res = meController.updateProfile(auth, req);
        assertEquals(200, res.getStatusCode().value());
        Map<String, Object> body = (Map<String, Object>) res.getBody();
        assertEquals("", body.get("avatarUrl"));
    }

    @Test
    void me_UnauthorizedMissingPrincipalEmail() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(Map.of("name", "no-email"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> meController.me(auth));
        assertEquals("Unauthorized", ex.getMessage());
    }

    @Test
    void meV2_Success() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(Map.of("email", "v2@example.com"));
        User user = mock(User.class);
        when(user.getId()).thenReturn(UUID.randomUUID());
        when(user.getEmail()).thenReturn("v2@example.com");
        when(user.getDisplayName()).thenReturn("Name");
        when(user.getAvatarUrl()).thenReturn("A");
        when(user.getRolesList()).thenReturn(Collections.singletonList("BUYER"));
        when(user.isEmailVerified()).thenReturn(true);
        when(user.getStatus()).thenReturn(id.ac.ui.cs.advprog.bidmart.backend.auth.entity.UserStatus.ACTIVE);
        when(user.getCreatedAt()).thenReturn(Instant.now());
        when(authService.getUserByEmail("v2@example.com")).thenReturn(user);

        ResponseEntity<?> res = meController.meV2(auth);
        assertEquals(200, res.getStatusCode().value());
    }

    @Test
    void updateProfileV2_Success() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(Map.of("email", "v2@example.com"));
        User user = mock(User.class);
        when(user.getDisplayName()).thenReturn("Updated");
        when(user.getAvatarUrl()).thenReturn("https://avatar");
        when(authService.getUserByEmail("v2@example.com")).thenReturn(user);
        when(authService.updateProfile(user, "Updated", "https://avatar")).thenReturn(user);

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.displayName = "Updated";
        req.avatarUrl = "https://avatar";

        ResponseEntity<?> res = meController.updateProfileV2(auth, req);
        assertEquals(200, res.getStatusCode().value());
    }

    @Test
    void updateProfileV2_AvatarUrlNull() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(Map.of("email", "v2@example.com"));
        User user = mock(User.class);
        when(user.getDisplayName()).thenReturn("Updated");
        when(user.getAvatarUrl()).thenReturn(null);
        when(authService.getUserByEmail("v2@example.com")).thenReturn(user);
        when(authService.updateProfile(user, "Updated", null)).thenReturn(user);

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.displayName = "Updated";
        req.avatarUrl = null;

        ResponseEntity<?> res = meController.updateProfileV2(auth, req);
        assertEquals(200, res.getStatusCode().value());
        Map<String, Object> body = (Map<String, Object>) res.getBody();
        assertEquals("", body.get("avatarUrl"));
    }

    @Test
    void changePassword_Success() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(Map.of("email", "pwd@example.com"));

        User user = mock(User.class);
        when(authService.getUserByEmail("pwd@example.com")).thenReturn(user);

        ChangePasswordRequestDTO req = new ChangePasswordRequestDTO();
        req.currentPassword = "oldpassword";
        req.newPassword = "newpassword";

        ResponseEntity<Map<String, String>> res = meController.changePassword(auth, req);
        assertEquals(200, res.getStatusCode().value());
        assertEquals("Password updated", res.getBody().get("message"));
        verify(authService).changePassword(user, req);
    }
}
