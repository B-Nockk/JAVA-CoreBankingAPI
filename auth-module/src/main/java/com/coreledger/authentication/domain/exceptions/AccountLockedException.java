// auth-module/src/main/java/com/coreledger/authentication/domain/exceptions/AccountLockedException.java
package com.coreledger.authentication.domain.exceptions;

import com.coreledger.authentication.domain.model.AuthFailureReason;

/**
 * Exception thrown when a user account is locked due to security policies.
 *
 * <p>
 * <b>Common Scenarios:</b>
 * <ul>
 * <li>Too many failed login attempts</li>
 * <li>Administrative lockout</li>
 * </ul>
 */
public class AccountLockedException extends AuthException {
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_MESSAGE = "Account is locked";
    private final AuthFailureReason reason;

    public AccountLockedException() {
        super(DEFAULT_MESSAGE);
        this.reason = AuthFailureReason.ACCOUNT_LOCKED;
    }

    public AccountLockedException(String correlationId) {
        super(DEFAULT_MESSAGE, correlationId);
        this.reason = AuthFailureReason.ACCOUNT_LOCKED;
    }

    @Override
    public String createLogMessage(String userId, String userEmail, String clientIp, String detailedReason) {
        return String.format(
                "Account locked - correlationId: %s, userId: %s, userEmail: %s, ip: %s, timestamp: %s, reason: %s",
                getCorrelationId(),
                userId != null ? userId : "[unknown]",
                userEmail != null ? userEmail : "[unknown]",
                clientIp != null ? clientIp : "[unknown]",
                getTimestamp(),
                reason,
                detailedReason != null ? detailedReason : "[unknown]");
    }
}
