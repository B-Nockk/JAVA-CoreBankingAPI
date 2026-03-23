// auth-module/src/main/java/com/coreledger/authentication/domain/exceptions/ReplayAttackException.java
package com.coreledger.authentication.domain.exceptions;

import lombok.Getter;

/**
 * Exception thrown when a replay attack is detected.
 *
 * <p>
 * <b>Common Scenarios:</b>
 * <ul>
 * <li>Request timestamp outside allowed window</li>
 * <li>Nonce already used</li>
 * </ul>
 *
 * @see AuthException
 */
@Getter
public class ReplayAttackException extends AuthException {

    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_MESSAGE = "Replay attack detected";

    private final String nonce;
    private final String requestTimestamp;

    public ReplayAttackException(String nonce, String requestTimestamp) {
        super(DEFAULT_MESSAGE);
        this.nonce = nonce;
        this.requestTimestamp = requestTimestamp;
    }

    public ReplayAttackException(String correlationId, String nonce, String requestTimestamp) {
        super(DEFAULT_MESSAGE, correlationId);
        this.nonce = nonce;
        this.requestTimestamp = requestTimestamp;
    }

    @Override
    public String createLogMessage(String userId, String userEmail, String clientIp, String detailedReason) {
        return String.format(
                "Replay attack - correlationId: %s, userId: %s, ip: %s, nonce: %s, requestTimestamp: %s, timestamp: %s, reason: %s",
                getCorrelationId(),
                userId != null ? userId : "[unknown]",
                userEmail != null ? userEmail : "[unknown]",
                clientIp != null ? clientIp : "[unknown]",
                nonce != null ? nonce : "[unknown]",
                requestTimestamp != null ? requestTimestamp : "[unknown]",
                getTimestamp(),
                detailedReason != null ? detailedReason : "[unknown]");
    }
}
