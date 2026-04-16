package id.ac.ui.cs.advprog.bidmart.backend.auth.dto;

import java.time.Instant;
import java.util.UUID;

public class SessionResponseDTO {
    public UUID id;
    public String device;
    public String ipAddress;
    public Instant lastActive;
    public boolean current;

    public SessionResponseDTO(UUID id, String device, String ipAddress, Instant lastActive, boolean current) {
        this.id = id;
        this.device = device;
        this.ipAddress = ipAddress;
        this.lastActive = lastActive;
        this.current = current;
    }
}
