// auth-module/src/main/java/com/coreledger/authentication/domain/exceptions/InvalidSignatureException.java
package com.coreledger.authentication.domain.exceptions;

import lombok.Getter;

/**
 * Exception thrown when a token or request signature is invalid.
 *
 * <p>
 * <b>Security Note:</b>
 * <ul>
 * <li>Never expose signature details to clients</li>
 * <li>Always log internally for auditing</li>
 * </ul>
 *
 * @see AuthException
 */
@Getter
public class InvalidSignatureException extends AuthException {

    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_MESSAGE = "Invalid signature";

    public InvalidSignatureException() {
        super(DEFAULT_MESSAGE);
    }

    public InvalidSignatureException(String correlationId) {
        super(DEFAULT_MESSAGE, correlationId);
    }

    @Override
    public String createLogMessage(String userId, String userEmail, String clientIp, String detailedReason) {
        // Base contract implementation (generic)
        return String.format(
                "Invalid signature - correlationId: %s, userId: %s, userEmail: %s, ip: %s, timestamp: %s, reason: %s",
                getCorrelationId(),
                userId != null ? userId : "[unknown]",
                userEmail != null ? userEmail : "[unknown]",
                clientIp != null ? clientIp : "[unknown]",
                getTimestamp(),
                detailedReason != null ? detailedReason : "[unknown]");
    }

    public String createLogMessage(String algorithm, String clientIp, String detailedReason) {
        return String.format(
                "Invalid signature - correlationId: %s, algorithm: %s, ip: %s, timestamp: %s, reason: %s",
                getCorrelationId(),
                algorithm != null ? algorithm : "[unknown]",
                clientIp != null ? clientIp : "[unknown]",
                getTimestamp(),
                detailedReason != null ? detailedReason : "[unknown]");
    }
}
