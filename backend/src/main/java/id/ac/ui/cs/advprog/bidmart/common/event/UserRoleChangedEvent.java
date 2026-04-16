package id.ac.ui.cs.advprog.bidmart.common.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class UserRoleChangedEvent {
    private final UUID userId;
    private final List<String> roles;
    private final Instant happenedAt;

    public UserRoleChangedEvent(UUID userId, List<String> roles, Instant happenedAt) {
        this.userId = userId;
        this.roles = roles;
        this.happenedAt = happenedAt;
    }

    public UUID getUserId() { return userId; }
    public List<String> getRoles() { return roles; }
    public Instant getHappenedAt() { return happenedAt; }
}
