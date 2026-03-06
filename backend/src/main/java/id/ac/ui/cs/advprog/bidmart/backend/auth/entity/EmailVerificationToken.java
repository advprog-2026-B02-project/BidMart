package id.ac.ui.cs.advprog.bidmart.backend.auth.entity;

import jakarta.persistence.*;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "email_verification_tokens", indexes = {
        @Index(name = "idx_email_verif_token", columnList = "token", unique = true)
})
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Setter
    @Column(nullable = false, unique = true, length = 120)
    private String token;

    @Setter
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Setter
    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }

    public User getUser() { return user; }

    public String getToken() { return token; }

    public Instant getExpiresAt() { return expiresAt; }

    public Instant getUsedAt() { return usedAt; }

    public Instant getCreatedAt() { return createdAt; }
}