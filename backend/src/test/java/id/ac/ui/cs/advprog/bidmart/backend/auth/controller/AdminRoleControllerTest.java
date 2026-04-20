package id.ac.ui.cs.advprog.bidmart.backend.auth.controller;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.RoleRequestDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.RoleResponseDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.service.AuthService;

@ExtendWith(MockitoExtension.class)
class AdminRoleControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AdminRoleController controller;

    @Test
    void listRoles() {
        RoleResponseDTO dto = new RoleResponseDTO(UUID.randomUUID(), "ADMIN", List.of("users:write"));
        when(authService.adminListRoles()).thenReturn(List.of(dto));

        ResponseEntity<List<RoleResponseDTO>> response = controller.listRoles();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void createRole() {
        RoleRequestDTO req = new RoleRequestDTO();
        req.name = "admin";
        req.permissions = List.of("users:write");

        RoleResponseDTO dto = new RoleResponseDTO(UUID.randomUUID(), "ADMIN", req.permissions);
        when(authService.adminCreateRole(req)).thenReturn(dto);

        ResponseEntity<RoleResponseDTO> response = controller.createRole(req);

        assertEquals(201, response.getStatusCode().value());
        assertEquals("ADMIN", response.getBody().name);
        verify(authService).adminCreateRole(req);
    }

    @Test
    void updateRole() {
        UUID roleId = UUID.randomUUID();
        RoleRequestDTO req = new RoleRequestDTO();
        req.name = "buyer";
        req.permissions = List.of("bid:read");

        RoleResponseDTO dto = new RoleResponseDTO(roleId, "BUYER", req.permissions);
        when(authService.adminUpdateRole(roleId, req)).thenReturn(dto);

        ResponseEntity<RoleResponseDTO> response = controller.updateRole(roleId, req);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(roleId, response.getBody().id);
        verify(authService).adminUpdateRole(roleId, req);
    }
}
