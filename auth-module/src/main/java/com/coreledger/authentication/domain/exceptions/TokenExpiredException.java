// auth-module/src/main/java/com/coreledger/authentication/domain/exceptions/TokenExpiredException.java
package com.coreledger.authentication.domain.exceptions;

import java.time.Instant;

import com.coreledger.authentication.domain.model.AuthFailureReason;

import lombok.Getter;

/**
 * Exception thrown when a token has expired and cannot be used.
 *
 * <p>
 * <b>Security Note:</b>
 * <ul>
 * <li>
 * This exception can be safely exposed to clients
 * (unlike {@link InvalidCredentialsException})</li>
 *
 * <li>
 * This exception deliberately provides vague messages
 * to prevent information leakage about the token or user.</li>
 *
 * <li>
 * Always log detailed reason internally</li>
 * </ul>
 *
 * <p>
 * <b>Common Scenarios:</b>
 * <ul>
 * <li>Refresh token rotation attempted with expired token</li>
 * <li>Access token validation fails due to expiration</li>
 * <li>Password reset token used after expiration window</li>
 * </ul>
 *
 * <p>
 * <b>Inherited Fields:</b>
 * {@inheritDoc}
 *
 * @see AuthException
 * @see InvalidCredentialsException
 * @see TokenReuseDetectedException
 */
@Getter
public class TokenExpiredException extends AuthException {

    /**
     * Default serial version UID for compatibility.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Generic error message used for all instances to avoid information leakage.
     *
     * <p>
     * <ul>
     * Deliberately doesn't specify what token is expired
     * </ul>
     */
    private static final String DEFAULT_MESSAGE = "Tokens Expired";

    /**
     * The exact timestamp when the token expired.
     *
     * <p>
     * This field is specific to token expiration and provides additional
     * context for debugging and logging.
     *
     * <p>
     * <b>Example:</b> 2024-01-15T10:30:00Z
     */
    private final Instant expiredAt;
    private final AuthFailureReason reason = AuthFailureReason.TOKEN_EXPIRED;

    /**
     * Creates a new exception with the default message.
     * Use this constructor for most cases where additional tracking is not needed
     *
     * @param expiredAt
     */
    public TokenExpiredException(Instant expiredAt) {
        super(DEFAULT_MESSAGE);
        this.expiredAt = expiredAt;
    }

    /**
     *
     * @param correlationId
     * @param expiredAt
     */
    public TokenExpiredException(String correlationId, Instant expiredAt) {
        super(DEFAULT_MESSAGE, correlationId);
        this.expiredAt = expiredAt;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * This implementation adds the expiredAt timestamp to the log message.
     *
     * @param userId         the user identifier (may be null)
     * @param userEmail      the user's email address (may be null)
     * @param clientIp       the client's IP address (may be null)
     * @param detailedReason the specific reason for failure (may be null)
     * @return formatted log message with token expiration context
     */
    public String createLogMessage(String userId, String userEmail, String clientIp, String detailedReason) {
        return String.format(
                "Token expired - correlationId: %s, userId: %s, userEmail: %s, ip: %s, expiredAt: %s, timestamp: %s, reason: %s",
                getCorrelationId(),
                userId != null ? userId : "[unknown]",
                userEmail != null ? userEmail : "[unknown]",
                clientIp != null ? clientIp : "[unknown]",
                expiredAt,
                getTimestamp(),
                reason,
                detailedReason != null ? detailedReason : "[unknown]");
    }

}
