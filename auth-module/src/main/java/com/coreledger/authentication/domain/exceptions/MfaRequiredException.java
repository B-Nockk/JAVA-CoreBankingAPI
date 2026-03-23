// auth-module/src/main/java/com/coreledger/authentication/domain/exceptions/MfaRequiredException.java
package com.coreledger.authentication.domain.exceptions;

import com.coreledger.authentication.domain.model.AuthFailureReason;

/**
 * Exception thrown when multi-factor authentication is required but not
 * provided.
 */
public class MfaRequiredException extends AuthException {
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_MESSAGE = "MFA required";
    private final AuthFailureReason reason;

    /**
     * Default constructor with generic message.
     */

    public MfaRequiredException() {
        super(DEFAULT_MESSAGE);
        this.reason = AuthFailureReason.MFA_REQUIRED;
    }

    public MfaRequiredException(String correlationId) {
        super(DEFAULT_MESSAGE, correlationId);
        this.reason = AuthFailureReason.MFA_REQUIRED;
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
                reason,
                detailedReason != null ? detailedReason : "[unknown]");
    }
}
