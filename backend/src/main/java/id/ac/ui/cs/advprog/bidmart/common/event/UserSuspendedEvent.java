package id.ac.ui.cs.advprog.bidmart.common.event;

import java.time.Instant;
import java.util.UUID;

public class UserSuspendedEvent {
    private final UUID userId;
    private final String reason;
    private final Instant happenedAt;

    public UserSuspendedEvent(UUID userId, String reason, Instant happenedAt) {
        this.userId = userId;
        this.reason = reason;
        this.happenedAt = happenedAt;
    }

    public UUID getUserId() { return userId; }
    public String getReason() { return reason; }
    public Instant getHappenedAt() { return happenedAt; }
}
