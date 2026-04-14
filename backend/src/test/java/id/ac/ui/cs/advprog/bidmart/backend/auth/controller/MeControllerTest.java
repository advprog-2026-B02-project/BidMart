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
}
