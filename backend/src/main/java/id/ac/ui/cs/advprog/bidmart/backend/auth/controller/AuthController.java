package id.ac.ui.cs.advprog.bidmart.backend.auth.controller;

import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.AuthResponse;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.LoginRequest;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.LoginRequestDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.RefreshRequest;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.RegisterRequest;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.RegisterRequestDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.TwoFactorConfirmRequestDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.TwoFactorDisableRequestDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.TwoFactorSetupRequestDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.TwoFactorVerifyRequestDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.UserResponseDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.VerifyEmailRequestDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.entity.User;
import id.ac.ui.cs.advprog.bidmart.backend.auth.service.AuthService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody RegisterRequestDTO req) {
        UserResponseDTO response = auth.registerAndReturn(req);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verify(@RequestParam("token") String token) {
        auth.verifyEmail(token);
        return ResponseEntity.ok("Email berhasil diverifikasi. Silakan login.");
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@Valid @RequestBody VerifyEmailRequestDTO req) {
        auth.verifyEmail(req.token);
        return ResponseEntity.ok(Map.of("message", "Email verified"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO req, HttpServletRequest servletRequest) {
        return ResponseEntity.ok(auth.loginWithDesign(req, servletRequest));
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<?> verifyTwoFactor(@Valid @RequestBody TwoFactorVerifyRequestDTO req,
                                             HttpServletRequest servletRequest) {
        return ResponseEntity.ok(auth.verifyTwoFactor(req, servletRequest));
    }

    @PostMapping("/2fa/setup")
    public ResponseEntity<?> setupTwoFactor(@Valid @RequestBody TwoFactorSetupRequestDTO req, Authentication authentication) {
        User user = getCurrentUser(authentication);
        return ResponseEntity.ok(auth.setupTwoFactor(user, req.method));
    }

    @PostMapping("/2fa/confirm")
    public ResponseEntity<Map<String, String>> confirmTwoFactor(@Valid @RequestBody TwoFactorConfirmRequestDTO req,
                                                                 Authentication authentication) {
        User user = getCurrentUser(authentication);
        auth.confirmTwoFactor(user, req.code);
        return ResponseEntity.ok(Map.of("message", "2FA enabled"));
    }

    @DeleteMapping("/2fa")
    public ResponseEntity<Map<String, String>> disableTwoFactor(@Valid @RequestBody TwoFactorDisableRequestDTO req,
                                                                 Authentication authentication) {
        User user = getCurrentUser(authentication);
        auth.disableTwoFactor(user, req.password);
        return ResponseEntity.ok(Map.of("message", "2FA disabled"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        return ResponseEntity.ok(auth.refresh(req.refreshToken));
    }

    @PostMapping("/refresh-v2")
    public ResponseEntity<?> refreshV2(@Valid @RequestBody RefreshRequest req) {
        return ResponseEntity.ok(auth.refreshWithDesign(req.refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshRequest req) {
        if (req != null && req.refreshToken != null && !req.refreshToken.isBlank()) {
            auth.logout(req.refreshToken);
        }
        return ResponseEntity.ok().build();
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("Unauthorized");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> principal = (Map<String, Object>) authentication.getPrincipal();
        Object email = principal.get("email");
        if (email == null) {
            throw new IllegalArgumentException("Unauthorized");
        }
        return auth.getUserByEmail(email.toString());
    }

    // Backward-compatible methods for existing tests and legacy call sites.
    public ResponseEntity<Void> register(RegisterRequest req) {
        auth.register(req.email, req.password, req.displayName);
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<AuthResponse> login(LoginRequest req) {
        return ResponseEntity.ok(auth.login(req.email, req.password));
    }

    @GetMapping("/{userId}/validate")
    public ResponseEntity<Void> validateUser(@PathVariable("userId") java.util.UUID userId) {
        auth.validateUser(userId);
        return ResponseEntity.ok().build();
    }
}