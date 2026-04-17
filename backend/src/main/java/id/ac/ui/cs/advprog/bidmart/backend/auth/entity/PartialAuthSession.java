package id.ac.ui.cs.advprog.bidmart.backend.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "partial_auth_sessions")
public class PartialAuthSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Setter
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Setter
    @Column(name = "partial_token", nullable = false, unique = true, length = 200)
    private String partialToken;

    @Setter
    @Column(name = "methods", nullable = false, length = 100)
    private String methods;

    @Setter
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Setter
    @Column(name = "used", nullable = false)
    private boolean used = false;

    @Setter
    @Column(name = "email_otp_hash", length = 100)
    private String emailOtpHash;

    @Setter
    @Column(name = "email_otp_expires_at")
    private Instant emailOtpExpiresAt;

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public String getPartialToken() { return partialToken; }
    public String getMethods() { return methods; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isUsed() { return used; }
    public String getEmailOtpHash() { return emailOtpHash; }
    public Instant getEmailOtpExpiresAt() { return emailOtpExpiresAt; }
}
