package id.ac.ui.cs.advprog.bidmart.backend.auth.controller;

import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.ChangePasswordRequestDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.UpdateProfileRequest;
import id.ac.ui.cs.advprog.bidmart.backend.auth.entity.User;
import id.ac.ui.cs.advprog.bidmart.backend.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class MeController {

    private final AuthService authService;

    public MeController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        return ResponseEntity.ok(profileMap(currentUser(auth)));
    }

    @GetMapping("/users/me")
    public ResponseEntity<?> meV2(Authentication auth) {
        return ResponseEntity.ok(profileMap(currentUser(auth)));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(Authentication auth, @Valid @RequestBody UpdateProfileRequest req) {
        User user = currentUser(auth);
        user = authService.updateProfile(user, req.displayName, req.avatarUrl);

        return ResponseEntity.ok(Map.of(
                "message", "Profil berhasil diperbarui",
                "displayName", user.getDisplayName(),
                "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : ""
        ));
    }

    @PutMapping("/users/me")
    public ResponseEntity<?> updateProfileV2(Authentication auth, @Valid @RequestBody UpdateProfileRequest req) {
        User user = currentUser(auth);
        user = authService.updateProfile(user, req.displayName, req.avatarUrl);

        return ResponseEntity.ok(Map.of(
                "message", "Profil berhasil diperbarui",
                "displayName", user.getDisplayName(),
                "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : ""
        ));
    }

    @PutMapping("/users/me/password")
    public ResponseEntity<Map<String, String>> changePassword(Authentication auth,
                                                              @Valid @RequestBody ChangePasswordRequestDTO req) {
        authService.changePassword(currentUser(auth), req);
        return ResponseEntity.ok(Map.of("message", "Password updated"));
    }

    private User currentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalArgumentException("Unauthorized");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> principal = (Map<String, Object>) auth.getPrincipal();
        Object email = principal.get("email");
        if (email == null) {
            throw new IllegalArgumentException("Unauthorized");
        }
        return authService.getUserByEmail(email.toString());
    }

    private Map<String, Object> profileMap(User user) {
        return Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "displayName", user.getDisplayName() != null ? user.getDisplayName() : "Pengguna Baru",
                "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
                "roles", user.getRolesList(),
                "emailVerified", user.isEmailVerified(),
                "status", user.getStatus().name(),
                "createdAt", user.getCreatedAt()
        );
    }
}