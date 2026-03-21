// auth-module/src/main/java/com/coreledger/authentication/domain/model/RefreshToken.java
package com.coreledger.authentication.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.coreledger.authentication.domain.exceptions.TokenReuseDetectedException;
import com.coreledger.shared.domain.UserId;

import lombok.Getter;

@Getter
public class RefreshToken {
    private final UUID tokenId;
    private final TokenHash tokenHash;
    private final UserId userId;
    private final Instant issuedAt;
    private final Instant expiredAt;

    // Kept nullable internally, exposed as Optional via getters below
    @Getter(lombok.AccessLevel.NONE)
    private final Instant revokedAt;

    @Getter(lombok.AccessLevel.NONE)
    private final UUID replacedByTokenId;

    private RefreshToken(
            UUID tokenId,
            TokenHash tokenHash,
            UserId userId,
            Instant issuedAt,
            Instant expiredAt,
            Instant revokedAt,
            UUID replacedByTokenId) {

        this.tokenId = Objects.requireNonNull(tokenId, "tokenId must not be null");
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt must not be null");
        this.expiredAt = Objects.requireNonNull(expiredAt, "expiredAt must not be null");

        this.revokedAt = revokedAt;
        this.replacedByTokenId = replacedByTokenId;
    }

    /**
     * Factory method for creating a BRAND NEW token.
     */
    public static RefreshToken createNew(
            TokenHash tokenHash,
            UserId userId,
            Instant issuedAt,
            Instant expiredAt) {
        return new RefreshToken(
                generateTokenId(),
                tokenHash,
                userId,
                issuedAt,
                expiredAt,
                null, // Not revoked yet
                null // Not replaced yet
        );
    }

    /**
     * Factory method for reconstituting an existing token from the database.
     */
    public static RefreshToken load(
            UUID tokenId,
            TokenHash tokenHash,
            UserId userId,
            Instant issuedAt,
            Instant expiredAt,
            Instant revokedAt,
            UUID replacedByTokenId) {
        return new RefreshToken(tokenId, tokenHash, userId, issuedAt, expiredAt, revokedAt, replacedByTokenId);
    }

    public static UUID generateTokenId() {
        return UUID.randomUUID(); // Syntax error fixed here
    }

    // --- Optional Getters ---

    public Optional<Instant> getRevokedAt() {
        return Optional.ofNullable(revokedAt);
    }

    public Optional<UUID> getReplacedByTokenId() {
        return Optional.ofNullable(replacedByTokenId);
    }

    // --- Domain Logic Methods ---

    public boolean isExpired() {
        return Instant.now().isAfter(expiredAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isActive() {
        return !isExpired() && !isRevoked();
    }

    /**
     * Since the class is immutable, revoking returns a NEW instance
     * with the updated revocation fields.
     */
    public RefreshToken revoke(Instant revokeTime, UUID replacedBy) {
        if (isRevoked()) {
            throw new IllegalStateException("Token is already revoked");
        }
        return new RefreshToken(
                this.tokenId,
                this.tokenHash,
                this.userId,
                this.issuedAt,
                this.expiredAt,
                revokeTime,
                replacedBy);
    }

    /**
     * Represents the result of a token rotation operation.
     *
     * @param oldToken     the revoked old token (now invalid for future use)
     * @param newToken     the newly created refresh token
     * @param rotationTime the instant when rotation occurred
     * @param wasExpired   indicates if the old token was expired (for logging)
     */
    public record TokenRotationResult(
            RefreshToken oldToken,
            RefreshToken newToken,
            Instant rotationTime,
            boolean wasExpired) {
        public TokenRotationResult(
                RefreshToken oldToken,
                RefreshToken newToken,
                Instant rotationTime) {
            this(oldToken, newToken, rotationTime, false);
        }
    }

    /**
     * Rotates this refresh token, creating a new token while revoking the current
     * one.
     *
     * <p>
     * <b>Important Side Effects:</b>
     * <ul>
     * <li>
     * If this token is already revoked, a {@link TokenReuseDetectedException} is
     * thrown.
     * This indicates a token reuse attempt and MUST trigger the revocation of ALL
     * tokens
     * belonging to the associated user as a security measure.
     * </li>
     * <li>
     * Upon successful rotation, the current token is marked as revoked and linked
     * to
     * the newly created token.
     * </li>
     * </ul>
     *
     * <p>
     * <b>Implementation Notes for Callers:</b>
     * <ul>
     * <li>This method returns a {@link TokenRotationResult} containing <b>both the
     * new token & the revoked (old) token</b>.</li>
     * <li>Callers are responsible for persisting <b>both</b> tokens in a single
     * database transaction to maintain consistency.</li>
     * <li>Due to the immutable nature of this class, the revocation state change is
     * represented by returning a new revoked instance.</li>
     * </ul>
     *
     * <p>
     * <b>Example Usage:</b>
     *
     * <pre>{@code
     * // In an Application Service:
     * TokenRotationResult result = existingToken.rotate(Instant.now(), newHash, Duration.ofDays(7));
     * * // IMPORTANT: Both tokens must be persisted atomically
     * tokenRepository.save(result.oldToken());
     * tokenRepository.save(result.newToken());
     * }</pre>
     *
     * <p>
     * <b>Security Considerations:</b>
     * <ul>
     * <li>When {@link TokenReuseDetectedException} is thrown, the application MUST
     * catch it and revoke all active tokens for {@code this.userId}.</li>
     * </ul>
     *
     * @param rotationTime the instant when the rotation occurs (typically
     *                     Instant.now())
     * @param tokenHash    the hashed representation of the new token for storage
     * @param timeToLive   the duration the new refresh token should remain valid
     *                     (e.g., 7 days)
     * @return A {@link TokenRotationResult} containing the revoked old token and
     *         the new token, ready for persistence
     * @throws TokenReuseDetectedException if this token was already revoked
     *                                     (indicates token reuse attack)
     * @throws IllegalStateException       if this token is expired and cannot be
     *                                     rotated
     *
     * @see TokenReuseDetectedException
     */
    public TokenRotationResult rotate(Instant rotationTime, TokenHash tokenHash, Duration timeToLive) {
        if (this.isRevoked()) {
            // THE CRITICAL CHECK: This token was already used/revoked!
            // Throwing this exception signals the Application Service to
            // query the DB and revoke ALL tokens belonging to this.userId
            throw new TokenReuseDetectedException(this.tokenId, this.userId);
        }

        if (this.isExpired()) {
            throw new IllegalStateException("Cannot rotate an expired token.");
        }

        RefreshToken newToken = RefreshToken.createNew(
                tokenHash,
                this.userId,
                rotationTime,
                rotationTime.plus(timeToLive));

        // 2. Revoke THIS current token and link it to the new one
        // (Note: Because this class is immutable, we return the new token
        // and let the service save both to the DB)
        RefreshToken revokedOldToken = this.revoke(rotationTime, newToken.getTokenId());

        return new TokenRotationResult(revokedOldToken, newToken, rotationTime);
    }
}