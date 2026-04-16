package id.ac.ui.cs.advprog.bidmart.backend.auth.entity;

import jakarta.persistence.*;
import lombok.Setter;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email", columnList = "email", unique = true)
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Setter
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Setter
    @Column(name = "display_name", length = 100)
    private String displayName;

    @Setter
    @Column(name = "avatar_url")
    private String avatarUrl;

    @Setter
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Setter
    @Column(name = "roles", nullable = false, length = 200, columnDefinition = "varchar(200) default 'BUYER,SELLER'")
    private String roles = "BUYER,SELLER";

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20, columnDefinition = "varchar(20) default 'ACTIVE'")
    private UserStatus status = UserStatus.ACTIVE;

    @Setter
    @Column(name = "two_factor_enabled", nullable = false, columnDefinition = "boolean default false")
    private boolean twoFactorEnabled = false;

    @Setter
    @Column(name = "two_factor_secret", length = 128)
    private String twoFactorSecret;

    @Setter
    @Column(name = "two_factor_temp_secret", length = 128)
    private String twoFactorTempSecret;

    @Setter
    @Column(name = "two_factor_backup_codes", columnDefinition = "TEXT")
    private String twoFactorBackupCodes;

    @Setter
    @Column(name = "two_factor_method", length = 20)
    private String twoFactorMethod;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email.toLowerCase().trim(); }

    public String getPasswordHash() { return passwordHash; }

    public String getDisplayName() { return displayName; }
    public String getAvatarUrl() { return avatarUrl; }

    public boolean isEmailVerified() { return emailVerified; }

    public String getRoles() { return roles; }
    public List<String> getRolesList() {
        if (roles == null || roles.isBlank()) {
            return List.of();
        }
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> s.toUpperCase(Locale.ROOT))
                .distinct()
                .collect(Collectors.toList());
    }
    public void setRolesList(List<String> roleNames) {
        this.roles = roleNames.stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> s.toUpperCase(Locale.ROOT))
                .distinct()
                .collect(Collectors.joining(","));
    }

    public UserStatus getStatus() { return status; }

    public boolean isTwoFactorEnabled() { return twoFactorEnabled; }
    public String getTwoFactorSecret() { return twoFactorSecret; }
    public String getTwoFactorTempSecret() { return twoFactorTempSecret; }
    public String getTwoFactorBackupCodes() { return twoFactorBackupCodes; }
    public String getTwoFactorMethod() { return twoFactorMethod; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}