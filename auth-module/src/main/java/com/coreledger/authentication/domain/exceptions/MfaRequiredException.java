// auth-module/src/main/java/com/coreledger/authentication/domain/exceptions/MfaRequiredException.java
package com.coreledger.authentication.domain.exceptions;

/**
 * Exception thrown when multi-factor authentication is required but not
 * provided.
 */
public class MfaRequiredException extends AuthException {
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_MESSAGE = "MFA required";

    public MfaRequiredException() {
        super(DEFAULT_MESSAGE);
    }

    public MfaRequiredException(String correlationId) {
        super(DEFAULT_MESSAGE, correlationId);
    }

    @Override
    public String createLogMessage(String userId, String userEmail, String clientIp, String detailedReason) {
        return String.format(
                "MFA required - correlationId: %s, userId: %s, userEmail: %s, ip: %s, timestamp: %s, reason: %s",
                getCorrelationId(),
                userId != null ? userId : "[unknown]",
                userEmail != null ? userEmail : "[unknown]",
                clientIp != null ? clientIp : "[unknown]",
                getTimestamp(),
                detailedReason != null ? detailedReason : "[unknown]");
    }
}
