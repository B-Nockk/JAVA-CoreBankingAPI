// auth-module/src/main/java/com/coreledger/authentication/domain/exceptions/UserAlreadyExistsException.java
package com.coreledger.authentication.domain.exceptions;

/**
 * Exception thrown when a signup attempt is made with an existing user account.
 *
 * <p>
 * <b>Common Scenarios:</b>
 * <ul>
 * <li>Email already registered</li>
 * <li>Username already taken</li>
 * </ul>
 */
public class UserAlreadyExistsException extends AuthException {
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_MESSAGE = "User already exists";

    public UserAlreadyExistsException() {
        super(DEFAULT_MESSAGE);
    }

    public UserAlreadyExistsException(String correlationId) {
        super(DEFAULT_MESSAGE, correlationId);
    }

    @Override
    public String createLogMessage(String userId, String userEmail, String clientIp, String detailedReason) {
        return String.format(
                "Signup failed - correlationId: %s, userId: %s, userEmail: %s, ip: %s, timestamp: %s, reason: %s",
                getCorrelationId(),
                userId != null ? userId : "[unknown]",
                userEmail != null ? userEmail : "[unknown]",
                clientIp != null ? clientIp : "[unknown]",
                getTimestamp(),
                detailedReason != null ? detailedReason : "[unknown]");
    }
}
