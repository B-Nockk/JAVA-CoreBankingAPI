// auth-module/src/main/java/com/coreledger/authentication/infrastructure/security/JwtTokenGeneratorAdapter.java
package com.coreledger.authentication.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

import org.springframework.stereotype.Component; // Assuming you use Spring

import com.coreledger.authentication.application.ports.out.TokenGeneratorPort;
import com.coreledger.authentication.domain.model.AccessToken;
import com.coreledger.authentication.domain.model.RawToken;
import com.coreledger.authentication.domain.model.TokenHash;
import com.coreledger.shared.domain.UserId;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

// @Component tells Spring to initialize this so you can inject it into your Application Service
@Component
public class JwtTokenGeneratorAdapter implements TokenGeneratorPort {

    private final SecureRandom secureRandom = new SecureRandom();

    // ========================================================================
    // 1. REFRESH TOKEN GENERATION (Opaque / Database-backed)
    // ========================================================================

    @Override
    public GeneratedRefreshToken generateRefreshToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String rawString = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        RawToken rawToken = new RawToken(rawString);
        TokenHash tokenHash = new TokenHash(hash(rawString));

        return new GeneratedRefreshToken(rawToken, tokenHash);
    }

    private String hash(String rawString) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawString.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is required", e);
        }
    }

    // ========================================================================
    // 2. ACCESS TOKEN GENERATION (JWT / RS256 Signed / Stateless)
    // ========================================================================

    @Override
    public AccessToken generateAccessToken(
            UserId userId,
            PrivateKey rsaPrivateKey) {
        return generateAccessToken(userId, rsaPrivateKey, 15, ChronoUnit.MINUTES);
    }

    @Override
    public AccessToken generateAccessToken(
            UserId userId,
            PrivateKey rsaPrivateKey,
            long ttl,
            ChronoUnit unit) {

        Instant now = Instant.now();
        Instant expiration = now.plus(ttl, unit);

        String jwtString = Jwts.builder()
                .setSubject(userId.toString())
                .claim("type", "ACCESS")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(rsaPrivateKey, SignatureAlgorithm.RS256)
                .compact();

        return AccessToken.of(jwtString, expiration);
    }
}
