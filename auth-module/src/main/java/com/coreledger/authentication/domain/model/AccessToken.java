// auth-module/src/main/java/com/coreledger/authentication/domain/model/AccessToken.java
package com.coreledger.authentication.domain.model;

import java.time.Instant;
import java.util.Objects;

import lombok.Getter;

@Getter
public class AccessToken {
    private final String tokenValue; // The raw JWT string
    private final Instant expiredAt;

    private AccessToken(String tokenValue, Instant expiredAt) {
        this.tokenValue = Objects.requireNonNull(tokenValue, "tokenValue must not be null");
        this.expiredAt = Objects.requireNonNull(expiredAt, "expiredAt must not be null");
    }

    public static AccessToken of(String tokenValue, Instant expiredAt) {
        return new AccessToken(tokenValue, expiredAt);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiredAt);
    }
}