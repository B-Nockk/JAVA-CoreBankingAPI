// auth-module/src/main/java/com/coreledger/authentication/domain/model/TokenPair.java
package com.coreledger.authentication.domain.model;

import java.util.Objects;

import lombok.Getter;

@Getter
public class TokenPair {
    private final AccessToken accessToken;
    private final RefreshToken refreshToken;

    private TokenPair(
            AccessToken accessToken,
            RefreshToken refreshToken) {
        this.accessToken = Objects.requireNonNull(accessToken, "accessToken must not be null");
        this.refreshToken = Objects.requireNonNull(refreshToken, "refreshToken must not be null");
    }

    public static TokenPair create(AccessToken accessToken, RefreshToken refreshToken) {
        return new TokenPair(accessToken, refreshToken);
    }

    // ======================================================================
    // Behavior (Delegated to the underlying objects)
    // ======================================================================

    public boolean isAccessTokenExpired() {
        return accessToken.isExpired();
    }

    public boolean isRefreshTokenExpired() {
        return refreshToken.isExpired();
    }

    public boolean isRefreshTokenRevoked() {
        return refreshToken.isRevoked();
    }
}