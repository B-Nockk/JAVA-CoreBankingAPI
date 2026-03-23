// auth-module/src/main/java/com/coreledger/authentication/domain/exceptions/AuthException.java
package com.coreledger.authentication.domain.exceptions;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;

/**
 * TODO::
 * look into logging frameworks (SLF4J, Logback, etc.) with MDC (Mapped Diagnostic Context)
 * or structured logging to automatically attach service name, environment, request ID, etc.
 */

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
 *
 * <p>
 * <b>Contract:</b>
 * <ul>
 * <li>All subclasses must implement {@link #createLogMessage} to provide
 * structured, detailed logging context.</li>
 * </ul>
 *
 * <p>
 * <b>Usage Examples:</b>
 * <ul>
 * <li>When <code>userId</code> and <code>userEmail</code> are known:
 *
 * <pre>
 * throw new InvalidCredentialsException()
 *         .createLogMessage("12345", "user@example.com", "192.168.1.10", "Password mismatch");
 * </pre>
 *
 * </li>
 *
 * <li>When <code>userId</code> is unknown but email is attempted:
 *
 * <pre>
 * throw new InvalidCredentialsException()
 *         .createLogMessage(null, "user@example.com", "192.168.1.10", "No matching account");
 * </pre>
 *
 * </li>
 *
 * <li>When both <code>userId</code> and <code>userEmail</code> are unavailable
 * (e.g., invalid token before parsing):
 *
 * <pre>
 * throw new InvalidSignatureException()
 *         .createLogMessage(null, null, "192.168.1.10", "Signature mismatch");
 * </pre>
 *
 * </li>
 *
 * </ul>
 * 
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

    /**
     * Contract for subclasses to provide structured log messages.
     *
     * <p>
     * This ensures every exception type has a consistent logging format
     * while allowing each subclass to add its own contextual fields.
     *
     * @param userId         the user identifier (may be null)
     * @param userEmail      the user's email address (may be null)
     * @param clientIp       the client's IP address (may be null)
     * @param detailedReason the specific reason for failure (may be null)
     * @return formatted log message with exception-specific context
     */
    public abstract String createLogMessage(String userId, String userEmail, String clientIp, String detailedReason);
}
