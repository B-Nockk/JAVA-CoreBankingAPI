// shared-kernel/src/main/java/com/coreledger/shared/domain/AuditMetadata.java
package com.coreledger.shared.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Value Object capturing the who/when of any significant domain action.
 *
 * Embedded into entities that require an audit trail — which in a banking
 * context means essentially everything. Rather than scattering createdAt,
 * createdBy, updatedAt fields across every class, we group them here as
 * a single cohesive concept.
 *
 * Immutable by design:
 * - createdAt / createdBy are set once at construction and never change
 * - When a record is "updated", a new record is created (append-only ledger).
 * There is no updatedAt here because in our domain, we don't update — we
 * append.
 *
 * Uses Instant (not LocalDateTime) because Instant is always UTC.
 * LocalDateTime has no timezone — dangerous in a multi-region financial system.
 */
public final class AuditMetadata {

    private final Instant createdAt;
    private final String createdBy;

    private AuditMetadata(Instant createdAt, String createdBy) {
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(createdBy, "createdBy must not be null or blank");
        if (createdBy.isBlank()) {
            throw new IllegalArgumentException("createdBy must not be blank");
        }
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    /**
     * Create audit metadata stamped at exactly now.
     * This is the standard factory for real operations.
     *
     * @param createdBy system identifier, user ID, or service name
     */
    public static AuditMetadata now(String createdBy) {
        return new AuditMetadata(Instant.now(), createdBy);
    }

    /**
     * Create audit metadata with an explicit timestamp.
     * Use for testing, replays, or event sourcing reconstruction.
     */
    public static AuditMetadata of(Instant createdAt, String createdBy) {
        return new AuditMetadata(createdAt, createdBy);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof AuditMetadata other))
            return false;
        return Objects.equals(createdAt, other.createdAt)
                && Objects.equals(createdBy, other.createdBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(createdAt, createdBy);
    }

    @Override
    public String toString() {
        return "AuditMetadata{createdAt=" + createdAt + ", createdBy='" + createdBy + "'}";
    }
}