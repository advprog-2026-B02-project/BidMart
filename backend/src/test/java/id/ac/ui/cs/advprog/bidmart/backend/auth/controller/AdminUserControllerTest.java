package id.ac.ui.cs.advprog.bidmart.backend.auth.controller;

import java.time.Instant;
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

import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.UpdateUserRolesRequestDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.UpdateUserStatusRequestDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.dto.UserResponseDTO;
import id.ac.ui.cs.advprog.bidmart.backend.auth.service.AuthService;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AdminUserController controller;

    @Test
    void listUsers() {
        UserResponseDTO dto = new UserResponseDTO(UUID.randomUUID(), "a@b.com", "name", true, Instant.now(), List.of("BUYER"));
        when(authService.adminListUsers("s", "BUYER", "ACTIVE", 1, 10)).thenReturn(List.of(dto));

        ResponseEntity<List<UserResponseDTO>> response = controller.listUsers(1, 10, "s", "BUYER", "ACTIVE");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        verify(authService).adminListUsers("s", "BUYER", "ACTIVE", 1, 10);
    }

    @Test
    void getUser() {
        UUID id = UUID.randomUUID();
        UserResponseDTO dto = new UserResponseDTO(id, "a@b.com", "name", true, Instant.now(), List.of("BUYER"));
        when(authService.adminGetUser(id)).thenReturn(dto);

        ResponseEntity<UserResponseDTO> response = controller.getUser(id);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(id, response.getBody().id);
    }

    @Test
    void updateStatus() {
        UUID id = UUID.randomUUID();
        UpdateUserStatusRequestDTO req = new UpdateUserStatusRequestDTO();
        req.status = "SUSPENDED";
        req.reason = "policy";

        UserResponseDTO dto = new UserResponseDTO(id, "a@b.com", "name", true, Instant.now(), List.of("BUYER"));
        when(authService.adminUpdateUserStatus(id, "SUSPENDED", "policy")).thenReturn(dto);

        ResponseEntity<UserResponseDTO> response = controller.updateStatus(id, req);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(id, response.getBody().id);
        verify(authService).adminUpdateUserStatus(id, "SUSPENDED", "policy");
    }

    @Test
    void updateRoles() {
        UUID id = UUID.randomUUID();
        UpdateUserRolesRequestDTO req = new UpdateUserRolesRequestDTO();
        req.roles = List.of("ADMIN", "BUYER");

        UserResponseDTO dto = new UserResponseDTO(id, "a@b.com", "name", true, Instant.now(), List.of("ADMIN", "BUYER"));
        when(authService.adminUpdateUserRoles(id, req.roles)).thenReturn(dto);

        ResponseEntity<UserResponseDTO> response = controller.updateRoles(id, req);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(2, response.getBody().roles.size());
        verify(authService).adminUpdateUserRoles(id, req.roles);
    }
}
