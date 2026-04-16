package id.ac.ui.cs.advprog.bidmart.backend.auth.controller;

import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.UpdateUserRolesRequestDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.UpdateUserStatusRequestDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.UserResponseDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AuthService authService;

    public AdminUserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> listUsers(@RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size,
                                                           @RequestParam(required = false) String search,
                                                           @RequestParam(required = false) String role,
                                                           @RequestParam(required = false) String status) {
        return ResponseEntity.ok(authService.adminListUsers(search, role, status, page, size));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponseDTO> getUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(authService.adminGetUser(userId));
    }

    @PutMapping("/{userId}/status")
    public ResponseEntity<UserResponseDTO> updateStatus(@PathVariable UUID userId,
                                                        @Valid @RequestBody UpdateUserStatusRequestDTO req) {
        return ResponseEntity.ok(authService.adminUpdateUserStatus(userId, req.status, req.reason));
    }

    @PutMapping("/{userId}/roles")
    public ResponseEntity<UserResponseDTO> updateRoles(@PathVariable UUID userId,
                                                       @Valid @RequestBody UpdateUserRolesRequestDTO req) {
        return ResponseEntity.ok(authService.adminUpdateUserRoles(userId, req.roles));
    }
}
