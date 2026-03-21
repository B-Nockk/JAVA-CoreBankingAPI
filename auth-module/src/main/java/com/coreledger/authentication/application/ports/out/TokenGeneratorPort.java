// auth-module/src/main/java/com/coreledger/authentication/application/ports/out/TokenGeneratorPort.java
package com.coreledger.authentication.application.ports.out;

import java.security.PrivateKey;
import java.time.temporal.ChronoUnit;

import com.coreledger.authentication.domain.model.AccessToken;
import com.coreledger.authentication.domain.model.RawToken;
import com.coreledger.authentication.domain.model.TokenHash;
import com.coreledger.shared.domain.UserId;

public interface TokenGeneratorPort {

    // The record can stay right here in the interface
    record GeneratedRefreshToken(RawToken rawToken, TokenHash tokenHash) {
    }

    GeneratedRefreshToken generateRefreshToken();

    AccessToken generateAccessToken(UserId userId, PrivateKey rsaPrivateKey);

    AccessToken generateAccessToken(UserId userId, PrivateKey rsaPrivateKey, long ttl, ChronoUnit unit);
}