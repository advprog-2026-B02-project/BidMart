package id.ac.ui.cs.advprog.bidmart.backend.auth.dto;

import java.util.List;
import java.util.UUID;

public class RoleResponseDTO {
    public UUID id;
    public String name;
    public List<String> permissions;

    public RoleResponseDTO(UUID id, String name, List<String> permissions) {
        this.id = id;
        this.name = name;
        this.permissions = permissions;
    }
}
