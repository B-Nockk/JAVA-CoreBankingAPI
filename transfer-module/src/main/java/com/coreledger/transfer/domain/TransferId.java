// transfer-module/src/main/java/com/coreledger/transfer/domain/TransferId.java
package com.coreledger.transfer.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object wrapping the unique identity of a Transfer.
 * Same pattern as AccountId — type safety over raw UUID.
 */
public final class TransferId {

    private final UUID value;

    private TransferId(UUID value) {
        Objects.requireNonNull(value, "TransferId value must not be null");
        this.value = value;
    }

    public static TransferId generate() {
        return new TransferId(UUID.randomUUID());
    }

    public static TransferId of(UUID value) {
        return new TransferId(value);
    }

    public static TransferId of(String value) {
        return new TransferId(UUID.fromString(value));
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof TransferId other))
            return false;
        return Objects.equals(value, other.value);
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