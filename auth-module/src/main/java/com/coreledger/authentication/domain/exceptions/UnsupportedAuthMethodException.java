// auth-module/src/main/java/com/coreledger/authentication/domain/exceptions/UnsupportedAuthMethodException.java
package com.coreledger.authentication.domain.exceptions;

/**
 * Exception thrown when an unsupported authentication method is attempted.
 */
public class UnsupportedAuthMethodException extends AuthException {
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_MESSAGE = "Unsupported authentication method";

    public UnsupportedAuthMethodException() {
        super(DEFAULT_MESSAGE);
    }

    public UnsupportedAuthMethodException(String correlationId) {
        super(DEFAULT_MESSAGE, correlationId);
    }

    @Override
    public String createLogMessage(String userId, String userEmail, String clientIp, String detailedReason) {
        return String.format(
                "Unsupported auth method - correlationId: %s, userId: %s, userEmail: %s, ip: %s, timestamp: %s, reason: %s",
                getCorrelationId(),
                userId != null ? userId : "[unknown]",
                userEmail != null ? userEmail : "[unknown]",
                clientIp != null ? clientIp : "[unknown]",
                getTimestamp(),
                detailedReason != null ? detailedReason : "[unknown]");
    }
}
