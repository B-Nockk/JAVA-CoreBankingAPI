// account-module/src/main/java/com/coreledger/account/domain/model/AccountId.java
package com.coreledger.account.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object wrapping the unique identity of an Account.
 *
 * Why not use UUID or String directly?
 * Type safety. AccountId and TransferId are both UUIDs under the hood,
 * but they are not interchangeable. Wrapping them in distinct types means
 * the compiler catches misuse — not just runtime tests.
 */
public final class AccountId {

    private final UUID value;

    private AccountId(UUID value) {
        Objects.requireNonNull(value, "AccountId value must not be null");
        this.value = value;
    }

    public static AccountId generate() {
        return new AccountId(UUID.randomUUID());
    }

    public static AccountId of(UUID value) {
        return new AccountId(value);
    }

    public static AccountId of(String value) {
        return new AccountId(UUID.fromString(value));
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof AccountId other))
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