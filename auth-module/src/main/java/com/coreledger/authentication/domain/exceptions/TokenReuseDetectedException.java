// auth-module/src/main/java/com/coreledger/authentication/domain/exceptions/TokenReuseDetectedException.java
package com.coreledger.authentication.domain.exceptions;

import java.util.UUID;

import com.coreledger.authentication.domain.model.AuthFailureReason;
import com.coreledger.shared.domain.UserId;
import lombok.Getter;

/**
 * Exception thrown when a token reuse attempt is detected.
 *
 * <p>
 * <b>Security Note:</b>
 * <ul>
 * <li>This is a critical security event — always log internally.</li>
 * <li>Correlation ID and timestamp are inherited from
 * {@link AuthException}.</li>
 * </ul>
 *
 * @see AuthException
 * @see TokenExpiredException
 * @see InvalidCredentialsException
 */
@Getter
public class TokenReuseDetectedException extends AuthException {

    private static final long serialVersionUID = 1L;

    private final UUID tokenId;
    private final UserId userId;
    private final AuthFailureReason reason = AuthFailureReason.TOKEN_REUSE_DETECTED;

    public TokenReuseDetectedException(UUID tokenId, UserId userId) {
        super("Security Alert: Token reuse detected");
        this.tokenId = tokenId;
        this.userId = userId;
    }

    public TokenReuseDetectedException(String correlationId, UUID tokenId, UserId userId) {
        super("Security Alert: Token reuse detected", correlationId);
        this.tokenId = tokenId;
        this.userId = userId;
    }

    /**
     * Creates a detailed log message for internal auditing.
     *
     * @return formatted log message with token reuse context
     */
    public String createLogMessage(String userId, String userEmail, String clientIp, String detailedReason) {
        return String.format(
                "Token reuse detected - correlationId: %s, userId: %s, tokenId: %s, timestamp: %s",
                getCorrelationId(),
                userId != null ? userId : "[unknown]",
                userEmail != null ? userEmail : "[unknown]",
                clientIp != null ? clientIp : "[unknown]",
                tokenId != null ? tokenId : "[unknown]",
                getTimestamp(),
                reason,
                detailedReason != null ? detailedReason : "[unknown]");
    }
}
