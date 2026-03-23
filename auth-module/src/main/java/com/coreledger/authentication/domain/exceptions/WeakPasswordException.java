// auth-module/src/main/java/com/coreledger/authentication/domain/exceptions/WeakPasswordException.java
package com.coreledger.authentication.domain.exceptions;

import com.coreledger.authentication.domain.model.AuthFailureReason;

/**
 * Exception thrown when a signup attempt uses a weak password.
 *
 * <p>
 * <b>Common Scenarios:</b>
 * <ul>
 * <li>Password does not meet complexity requirements</li>
 * <li>Password found in breach databases</li>
 * </ul>
 */
public class WeakPasswordException extends AuthException {
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_MESSAGE = "Weak password";
    private final AuthFailureReason reason = AuthFailureReason.WEAK_PASSWORD;

    public WeakPasswordException() {
        super(DEFAULT_MESSAGE);
    }

    public WeakPasswordException(String correlationId) {
        super(DEFAULT_MESSAGE, correlationId);
    }

    @Override
    public String createLogMessage(String userId, String userEmail, String clientIp, String detailedReason) {
        return String.format(
                "Weak password - correlationId: %s, userId: %s, userEmail: %s, ip: %s, timestamp: %s, reason: %s",
                getCorrelationId(),
                userId != null ? userId : "[unknown]",
                userEmail != null ? userEmail : "[unknown]",
                clientIp != null ? clientIp : "[unknown]",
                getTimestamp(),
                reason,
                detailedReason != null ? detailedReason : "[unknown]");
    }
}
