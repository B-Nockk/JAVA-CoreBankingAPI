// auth-module/src/main/java/com/coreledger/authentication/domain/exceptions/AuthException.java
package com.coreledger.authentication.domain.exceptions;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;

/**
 * Base exception for all authentication-related failures.
 *
 * <p>
 * This abstract class provides common fields and behavior for all
 * authentication exceptions in the domain layer.
 *
 * <p>
 * <b>Fields:</b>
 * <ul>
 * <li>{@link #correlationId} - Unique identifier for tracing across
 * services</li>
 * <li>{@link #timestamp} - When the exception was created</li>
 * </ul>
 *
 * @author Nockk
 * @since 1.0
 */
@Getter
public abstract class AuthException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    /**
     * Unique identifier for tracing this exception across service boundaries.
     *
     * <p>
     * This ID is generated automatically if not provided and can be:
     * <ul>
     * <li>Returned to clients in response headers (never in body for security)</li>
     * <li>Used to correlate logs across distributed systems</li>
     * <li>Helps in debugging production issues</li>
     * </ul>
     *
     * <p>
     * <b>Example:</b> "f47ac10b-58cc-4372-a567-0e02b2c3d479"
     */
    private final String correlationId;

    /**
     * Timestamp when this exception was created.
     *
     * <p>
     * Useful for:
     * <ul>
     * <li>Auditing when authentication failures occurred</li>
     * <li>Determining if failures happened before/after certain events</li>
     * <li>Log analysis and time-based correlation</li>
     * </ul>
     */
    private final Instant timestamp;

    protected AuthException(String message, String correlationId) {
        super(message);
        this.correlationId = correlationId != null ? correlationId : UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }

    protected AuthException(String message) {
        this(message, null);
    }
}
