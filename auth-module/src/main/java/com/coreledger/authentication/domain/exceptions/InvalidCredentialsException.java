// auth-module/src/main/java/com/coreledger/authentication/domain/exceptions/InvalidCredentialsException.java
package com.coreledger.authentication.domain.exceptions;

import java.time.Instant;
import java.util.UUID;

// TODO:: FIx Exceptions across the app
/**
 * Exception thrown when authentication fails due to invalid credentials.
 *
 * <p>
 * <b>Security Design:</b> This exception deliberately uses vague messaging
 * to prevent information leakage that could aid attackers. It does NOT indicate
 * whether the username, password, or other credential was invalid - only that
 * authentication failed.
 *
 * <p>
 * <b>When to Use:</b>
 * <ul>
 * <li>User provides incorrect password during login</li>
 * <li>Non-existent username is submitted</li>
 * <li>Account is locked/deactivated (to avoid leaking account status)</li>
 * <li>Multi-factor authentication fails</li>
 * </ul>
 *
 * <p>
 * <b>What NOT to Use This For:</b>
 * <ul>
 * <li>Token expiration or validation failures - use
 * {@link TokenExpiredException}</li>
 * <li>Missing required credentials - use
 * {@link MissingCredentialException}</li>
 * <li>Authorization failures - use {@link UnauthorizedException}</li>
 * </ul>
 *
 * <p>
 * <b>Security Best Practices:</b>
 * <ul>
 * <li>Always log detailed failure reasons internally (including username, IP,
 * timestamp)
 * for security auditing while returning this generic exception to clients</li>
 * <li>Consider implementing rate limiting on authentication attempts when this
 * exception occurs repeatedly from the same source</li>
 * <li>Include a correlation ID in logs to trace authentication failures without
 * exposing details in responses</li>
 * </ul>
 *
 * <p>
 * <b>Example Usage:</b>
 *
 * <pre>{@code
 * // In authentication service:
 * public User authenticate(String username, String password) {
 *     User user = userRepository.findByUsername(username);
 *
 *     if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
 *         // Log detailed reason internally
 *         log.warn("Authentication failed for username: {}, reason: {}",
 *                 username, user == null ? "user not found" : "invalid password");
 *
 *         // Return generic exception to client
 *         throw new InvalidCredentialsException();
 *     }
 *
 *     return user;
 * }
 *
 * // In controller advice:
 * @ExceptionHandler(InvalidCredentialsException.class)
 * public ResponseEntity<ErrorResponse> handleInvalidCredentials() {
 *     return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
 *             .body(new ErrorResponse("Invalid credentials provided"));
 * }
 * }</pre>
 *
 * <p>
 * <b>Logging Recommendation:</b>
 *
 * <pre>{@code
 * // Always include correlation ID for traceability
 * String correlationId = UUID.randomUUID().toString();
 * log.error("Authentication failed - correlationId: {}, username: {}, ip: {}, reason: {}",
 *         correlationId, username, clientIp, detailedReason);
 * }</pre>
 *
 * @see MissingCredentialException
 * @see TokenExpiredException
 * @see UnauthorizedException
 * @since 1.0
 */
public class InvalidCredentialsException extends RuntimeException {

    /**
     * Default serial version UID for compatibility.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Generic error message used for all instances to avoid information leakage.
     */
    private static final String DEFAULT_MESSAGE = "Invalid credentials provided";

    /**
     * Optional correlation ID for tracing this failure across logs.
     */
    private final String correlationId;

    /**
     * Timestamp when this exception was created.
     */
    private final Instant timestamp;

    /**
     * Creates a new exception with the default message.
     * Use this constructor for most cases where no additional tracking is needed.
     */
    public InvalidCredentialsException() {
        this(null);
    }

    /**
     * Creates a new exception with a correlation ID for tracing.
     *
     * <p>
     * Use this constructor when you need to correlate client responses
     * with internal logs without exposing details to the client.
     *
     * @param correlationId unique identifier to trace this failure in logs
     */
    public InvalidCredentialsException(String correlationId) {
        super(DEFAULT_MESSAGE);
        this.correlationId = correlationId != null ? correlationId : UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }

    /**
     * Gets the correlation ID for tracing this exception.
     *
     * @return the unique correlation identifier
     */
    public String getCorrelationId() {
        return correlationId;
    }

    /**
     * Gets the timestamp when this exception was created.
     *
     * @return the timestamp of exception creation
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Creates a detailed log message for internal auditing.
     *
     * <p>
     * This method should only be used for logging and never exposed to clients.
     *
     * @param username       the username that was attempted (may be null)
     * @param clientIp       the client IP address
     * @param detailedReason the specific reason for failure
     * @return a formatted log message suitable for security auditing
     */
    public String createLogMessage(String username, String clientIp, String detailedReason) {
        return String.format(
                "Authentication failed - correlationId: %s, username: %s, ip: %s, timestamp: %s, reason: %s",
                correlationId,
                username != null ? username : "[unknown]",
                clientIp != null ? clientIp : "[unknown]",
                timestamp,
                detailedReason != null ? detailedReason : "[unknown]");
    }
}
