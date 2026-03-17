// user-module/src/main/java/com/coreledger/user/domain/model/KycDocumentId.java
package com.coreledger.user.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing the unique identifier of a KYC document.
 * Strongly typed instead of using raw UUID.
 */
public final class KycDocumentId {
    private final UUID value;

    private KycDocumentId(UUID value) {
        this.value = Objects.requireNonNull(value, "DocumentId cannot be null");
    }

    public static KycDocumentId generate() {
        return new KycDocumentId(UUID.randomUUID());
    }

    public static KycDocumentId of(UUID value) {
        return new KycDocumentId(value);
    }

    public static KycDocumentId of(String value) {
        return new KycDocumentId(UUID.fromString(value));
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof KycDocumentId other) && value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
