package id.ac.ui.cs.advprog.bidmart.backend.auth.controller;

import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.SessionResponseDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.entity.User;
import id.ac.ui.cs.advprog.bidmart.backend.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;
import java.util.List;

@RestController
public class SessionController {

    private final AuthService authService;

    public SessionController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/users/me/sessions")
    public ResponseEntity<List<SessionResponseDTO>> listSessions(Authentication authentication) {
        User user = currentUser(authentication);
        String currentSessionId = currentSessionId(authentication);
        return ResponseEntity.ok(authService.getActiveSessions(user, currentSessionId));
    }

    @DeleteMapping("/users/me/sessions/{sessionId}")
    public ResponseEntity<Void> revokeSession(@PathVariable("sessionId") UUID sessionId, Authentication authentication) {
        User user = currentUser(authentication);
        authService.revokeSession(user, sessionId);
        return ResponseEntity.noContent().build();
    }

    // Backward-compatible endpoint method used by existing tests.
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> principal = (Map<String, Object>) authentication.getPrincipal();

        return ResponseEntity.ok(Map.of(
                "userId", principal.get("userId"),
                "email", principal.get("email")
        ));
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("Unauthorized");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> principal = (Map<String, Object>) authentication.getPrincipal();
        Object email = principal.get("email");
        if (email == null) {
            throw new IllegalArgumentException("Unauthorized");
        }
        return authService.getUserByEmail(email.toString());
    }

    private String currentSessionId(Authentication authentication) {
        @SuppressWarnings("unchecked")
        Map<String, Object> principal = (Map<String, Object>) authentication.getPrincipal();
        Object sid = principal.get("sessionId");
        return sid == null ? null : sid.toString();
    }
}