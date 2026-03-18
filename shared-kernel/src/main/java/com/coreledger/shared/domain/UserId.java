// user-module/src/main/java/com/coreledger/user/domain/model/UserId.java
package com.coreledger.shared.domain;

import java.util.Objects;
import java.util.UUID;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserId {

    @EqualsAndHashCode.Include
    private final UUID value;

    private UserId(UUID value) {
        Objects.requireNonNull(value, "UserId value must be a valid uuid");
        this.value = value;
    }

    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId of(UUID value) {
        return new UserId(value);
    }

    public static UserId of(String value) {
        return new UserId(UUID.fromString(value));
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

}
