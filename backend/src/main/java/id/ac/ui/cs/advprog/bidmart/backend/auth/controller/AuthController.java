package id.ac.ui.cs.advprog.bidmart.backend.auth.controller;

import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.AuthResponse;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.LoginRequest;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.LoginRequestDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.RefreshRequest;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.RegisterRequest;
import id.ac.ui.cs.advprog.bidmart.backend.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(auth.login(req.email, req.password));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        return ResponseEntity.ok(auth.refresh(req.refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest req) {
        auth.logout(req.refreshToken);
        return ResponseEntity.ok().build();
    }
}