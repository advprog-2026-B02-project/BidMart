package id.ac.ui.cs.advprog.bidmart.backend.auth.controller;

import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.UpdateProfileRequest;
import id.ac.ui.cs.advprog.bidmart.backend.auth.entity.User;
import id.ac.ui.cs.advprog.bidmart.backend.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class MeControllerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MeController meController;

    @Test
    void me_NoAuth() {
        ResponseEntity<?> res = meController.me(null);
        assertEquals(401, res.getStatusCode().value());
    }

    @Test
    void me_Unauthenticated() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);
        ResponseEntity<?> res = meController.me(auth);
        assertEquals(401, res.getStatusCode().value());
    }

    @Test
    void me_NotFound() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("e");
        when(userRepository.findByEmail("e")).thenReturn(Optional.empty());

        ResponseEntity<?> res = meController.me(auth);
        assertEquals(404, res.getStatusCode().value());
    }

    @Test
    void me_Success() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("e");
        User user = new User();
        user.setEmail("e");
        user.setDisplayName("D");
        user.setAvatarUrl("A");
        when(userRepository.findByEmail("e")).thenReturn(Optional.of(user));

        ResponseEntity<?> res = meController.me(auth);
        assertEquals(200, res.getStatusCode().value());
    }

    @Test
    void me_Success_DefaultValues() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("e");
        User user = new User();
        user.setEmail("e");
        user.setDisplayName(null);
        user.setAvatarUrl(null);
        when(userRepository.findByEmail("e")).thenReturn(Optional.of(user));

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
        when(auth.getName()).thenReturn("e");
        User user = new User();
        user.setEmail("e");
        when(userRepository.findByEmail("e")).thenReturn(Optional.of(user));

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.displayName = "N";
        req.avatarUrl = "U";

        ResponseEntity<?> res = meController.updateProfile(auth, req);
        assertEquals(200, res.getStatusCode().value());
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_NoAuth() {
        ResponseEntity<?> res = meController.updateProfile(null, new UpdateProfileRequest());
        assertEquals(401, res.getStatusCode().value());
    }

    @Test
    void updateProfile_NotFound() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("e");
        when(userRepository.findByEmail("e")).thenReturn(Optional.empty());

        ResponseEntity<?> res = meController.updateProfile(auth, new UpdateProfileRequest());
        assertEquals(404, res.getStatusCode().value());
    }

    @Test
    void updateProfile_AvatarUrlNull() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("e");
        User user = new User();
        user.setEmail("e");
        when(userRepository.findByEmail("e")).thenReturn(Optional.of(user));

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.displayName = "N";
        req.avatarUrl = null;

        ResponseEntity<?> res = meController.updateProfile(auth, req);
        assertEquals(200, res.getStatusCode().value());
        Map<String, Object> body = (Map<String, Object>) res.getBody();
        assertEquals("", body.get("avatarUrl"));
    }
}
