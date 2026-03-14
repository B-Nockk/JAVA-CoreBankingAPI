package com.coreledger.user.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing the unique identity of a KYC Profile.
 * Immutable and always backed by a UUID.
 */
public final class KycProfileId {

    private final UUID id;

    private KycProfileId(UUID id) {
        this.id = Objects.requireNonNull(id, "KycProfileId cannot be null");
    }

    /**
     * Factory method to generate a new KycProfileId.
     */
    public static KycProfileId generate() {
        return new KycProfileId(UUID.randomUUID());
    }

    /**
     * Factory method to restore an existing KycProfileId from persistence.
     */
    public static KycProfileId from(UUID id) {
        return new KycProfileId(id);
    }

    public UUID getValue() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof KycProfileId))
            return false;
        KycProfileId other = (KycProfileId) o;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
