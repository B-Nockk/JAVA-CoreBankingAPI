// auth-module/src/main/java/com/coreledger/authentication/domain/model/HmacCredential.java
// auth-module/src/main/java/com/coreledger/authentication/domain/model/HmacCredential.java
package com.coreledger.authentication.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import com.coreledger.shared.domain.UserId;
import lombok.Getter;

@Getter
public class HmacCredential {
    private final UUID credentialId;
    private final String secretKey; // Base64 encoded 256-bit secret
    private final UserId userId;
    private final String algorithm; // e.g., "HmacSHA256"
    private final boolean active;
    private final Instant createdAt;

    private HmacCredential(
            UUID credentialId,
            String secretKey,
            UserId userId,
            String algorithm,
            boolean active,
            Instant createdAt) {
        this.credentialId = Objects.requireNonNull(credentialId, "credentialId cannot be null");
        this.secretKey = Objects.requireNonNull(secretKey, "secretKey cannot be null");
        this.userId = Objects.requireNonNull(userId, "userId cannot be null");
        this.algorithm = Objects.requireNonNull(algorithm, "algorithm cannot be null");
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
    }

    // Factory for creating a brand new credential
    public static HmacCredential createNew(String secretKey, UserId userId) {
        return new HmacCredential(
                UUID.randomUUID(),
                secretKey,
                userId,
                "HmacSHA256", // Explicitly defined, never implicit
                true,
                Instant.now());
    }

    // Factory for reconstituting from DB
    public static HmacCredential load(
            UUID credentialId, String secretKey, UserId userId,
            String algorithm, boolean active, Instant createdAt) {
        return new HmacCredential(credentialId, secretKey, userId, algorithm, active, createdAt);
    }

    // Domain Behavior
    public HmacCredential deactivate() {
        if (!this.active) {
            throw new IllegalStateException("Credential is already inactive");
        }
        return new HmacCredential(
                this.credentialId, this.secretKey, this.userId,
                this.algorithm, false, this.createdAt);
    }

    public boolean isActive() {
        return this.active;
    }
}