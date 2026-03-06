package id.ac.ui.cs.advprog.bidmart.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SessionController {

    @GetMapping("/users/me")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "message", "Unauthorized"
            ));
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> principal = (Map<String, Object>) authentication.getPrincipal();

        return ResponseEntity.ok(Map.of(
                "userId", principal.get("userId"),
                "email", principal.get("email")
        ));
    }
}