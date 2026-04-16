package id.ac.ui.cs.advprog.bidmart.backend.auth.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class UserResponseDTO {
    public UUID id;
    public String email;
    public String displayName;
    public boolean emailVerified;
    public Instant createdAt;
    public List<String> roles;

    public UserResponseDTO(UUID id, String email, String displayName, boolean emailVerified, Instant createdAt, List<String> roles) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.emailVerified = emailVerified;
        this.createdAt = createdAt;
        this.roles = roles;
    }
}
