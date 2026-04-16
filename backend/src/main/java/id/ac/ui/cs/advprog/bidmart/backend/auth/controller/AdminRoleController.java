package id.ac.ui.cs.advprog.bidmart.backend.auth.controller;

import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.RoleRequestDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.RoleResponseDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/roles")
public class AdminRoleController {

    private final AuthService authService;

    public AdminRoleController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<RoleResponseDTO>> listRoles() {
        return ResponseEntity.ok(authService.adminListRoles());
    }

    @PostMapping
    public ResponseEntity<RoleResponseDTO> createRole(@Valid @RequestBody RoleRequestDTO req) {
        return ResponseEntity.status(201).body(authService.adminCreateRole(req));
    }

    @PutMapping("/{roleId}")
    public ResponseEntity<RoleResponseDTO> updateRole(@PathVariable UUID roleId,
                                                      @Valid @RequestBody RoleRequestDTO req) {
        return ResponseEntity.ok(authService.adminUpdateRole(roleId, req));
    }
}
