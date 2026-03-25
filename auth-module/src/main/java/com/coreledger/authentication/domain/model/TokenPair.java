// auth-module/src/main/java/com/coreledger/authentication/domain/model/TokenPair.java
package com.coreledger.authentication.domain.model;

import java.util.Objects;

import lombok.Getter;

@Getter
public class TokenPair {
    private final AccessToken accessToken;
    private final RefreshToken refreshToken;
    private final String rawRefreshToken;

    private TokenPair(
            AccessToken accessToken,
            RefreshToken refreshToken,
            String rawRefreshToken) {
        this.accessToken = Objects.requireNonNull(accessToken, "accessToken must not be null");
        this.refreshToken = Objects.requireNonNull(refreshToken, "refreshToken must not be null");
        this.rawRefreshToken = rawRefreshToken;
    }

    public static TokenPair create(AccessToken accessToken, RefreshToken refreshToken, String rawRefreshToken) {
        return new TokenPair(accessToken, refreshToken, rawRefreshToken);
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