package id.ac.ui.cs.advprog.bidmart.backend.auth.entity;

import jakarta.persistence.*;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_token_value", columnList = "token", unique = true),
        @Index(name = "idx_refresh_token_user", columnList = "user_id")
})
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Setter
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Setter
    @Column(nullable = false, unique = true, length = 200)
    private String token;

    @Setter
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Setter
    @Column(nullable = false)
    private boolean revoked = false;

    @Setter
    @Column(name = "device", length = 200)
    private String device;

    @Setter
    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Setter
    @Column(name = "last_active", nullable = false, columnDefinition = "timestamp with time zone default now()")
    private Instant lastActive = Instant.now();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }

    public User getUser() { return user; }

    public String getToken() { return token; }

    public Instant getExpiresAt() { return expiresAt; }

    public boolean isRevoked() { return revoked; }

    public String getDevice() { return device; }

    public String getIpAddress() { return ipAddress; }

    public Instant getLastActive() { return lastActive; }

    public Instant getCreatedAt() { return createdAt; }
}