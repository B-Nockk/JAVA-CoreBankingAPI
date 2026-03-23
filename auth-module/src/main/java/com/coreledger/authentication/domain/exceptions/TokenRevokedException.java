// auth-module/src/main/java/com/coreledger/authentication/domain/exceptions/TokenRevokedException.java
package com.coreledger.authentication.domain.exceptions;

import com.coreledger.authentication.domain.model.AuthFailureReason;

import lombok.Getter;

/**
 * Exception thrown when a token has been revoked and cannot be used.
 *
 * <p>
 * <b>Common Scenarios:</b>
 * <ul>
 * <li>User logs out and refresh token is revoked</li>
 * <li>Refresh token rotation invalidates old tokens</li>
 * </ul>
 *
 * @see AuthException
 */
@Getter
public class TokenRevokedException extends AuthException {

    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_MESSAGE = "Token has been revoked";
    private final AuthFailureReason reason = AuthFailureReason.TOKEN_REVOKED;

    public TokenRevokedException() {
        super(DEFAULT_MESSAGE);
    }

    public TokenRevokedException(String correlationId) {
        super(DEFAULT_MESSAGE, correlationId);
    }

    public String createLogMessage(String userId, String userEmail, String clientIp, String detailedReason) {
        return String.format(
                "Token revoked - correlationId: %s, userId: %s, ip: %s, timestamp: %s, reason: %s",
                getCorrelationId(),
                userId != null ? userId : "[unknown]",
                userEmail != null ? userEmail : "[unknown]",
                clientIp != null ? clientIp : "[unknown]",
                getTimestamp(),
                reason,
                detailedReason != null ? detailedReason : "[unknown]");
    }
}
