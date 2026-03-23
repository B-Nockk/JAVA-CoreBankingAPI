// auth-module/src/main/java/com/coreledger/authentication/domain/exceptions/MfaFailedException.java
package com.coreledger.authentication.domain.exceptions;

import com.coreledger.authentication.domain.model.AuthFailureReason;

/**
 * Exception thrown when multi-factor authentication fails.
 */
public class MfaFailedException extends AuthException {
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_MESSAGE = "MFA failed";
    private final AuthFailureReason reason;

    public MfaFailedException() {
        super(DEFAULT_MESSAGE);
        this.reason = AuthFailureReason.MFA_FAILED;
    }

    public MfaFailedException(String correlationId) {
        super(DEFAULT_MESSAGE, correlationId);
        this.reason = AuthFailureReason.MFA_FAILED;
    }

    @Override
    public String createLogMessage(String userId, String userEmail, String clientIp, String detailedReason) {
        return String.format(
                "MFA failed - correlationId: %s, userId: %s, userEmail: %s, ip: %s, timestamp: %s, reason: %s",
                getCorrelationId(),
                userId != null ? userId : "[unknown]",
                userEmail != null ? userEmail : "[unknown]",
                clientIp != null ? clientIp : "[unknown]",
                getTimestamp(),
                reason,
                detailedReason != null ? detailedReason : "[unknown]");
    }
}
