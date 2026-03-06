// app/src/main/java/com/coreledger/common/GlobalExceptionHandler.java
package com.coreledger.common.exception;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.coreledger.account.domain.exceptions.AccountNotFoundException;
import com.coreledger.account.domain.exceptions.InsufficientFundsException;
import com.coreledger.account.domain.exceptions.InvalidAccountOperationException;

/**
 * Global exception handler — maps domain and validation exceptions to
 * consistent HTTP error responses.
 *
 * Lives in common/ because it will handle exceptions from all modules
 * as the project grows.
 *
 * ErrorResponse is a consistent envelope for all error responses:
 * - status: HTTP status code (for clients that can't read the status line)
 * - message: human-readable description safe to expose externally
 * - timestamp: when the error occurred (useful for log correlation)
 * - errors: field-level validation errors (only populated for 400s)
 *
 * Security note on InsufficientFundsException:
 * We log the full detail internally (available balance, requested amount)
 * but the HTTP response only says "Insufficient funds" — no amounts.
 * Revealing exact balances in error responses is a data exposure risk.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 404 — account not found
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFound(AccountNotFoundException ex) {
        log.warn("Account not found: {}", ex.getIdentifier());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    // 422 — insufficient funds
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFunds(InsufficientFundsException ex) {
        // Log detail internally — never expose balance in the response
        log.warn("Insufficient funds — available: {}, requested: {}",
                ex.getAvailableBalance(), ex.getRequestedAmount());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient funds"));
    }

    // 422 — invalid account operation (frozen, closed, currency mismatch)
    @ExceptionHandler(InvalidAccountOperationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOperation(InvalidAccountOperationException ex) {
        log.warn("Invalid account operation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage()));
    }

    // 400 — bean validation failures (@Valid on request bodies)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationFailures(
            MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();

        log.warn("Validation failed: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.ofErrors(HttpStatus.BAD_REQUEST, "Validation failed", errors));
    }

    // 500 — catch-all for anything unhandled
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR,
                        "An unexpected error occurred"));
    }

    // -------------------------------------------------------------------------
    // Error response envelope
    // -------------------------------------------------------------------------

    public record ErrorResponse(
            int status,
            String message,
            Instant timestamp,
            List<String> errors) {
        static ErrorResponse of(HttpStatus status, String message) {
            return new ErrorResponse(status.value(), message, Instant.now(), List.of());
        }

        static ErrorResponse ofErrors(HttpStatus status, String message, List<String> errors) {
            return new ErrorResponse(status.value(), message, Instant.now(), errors);
        }
    }
}