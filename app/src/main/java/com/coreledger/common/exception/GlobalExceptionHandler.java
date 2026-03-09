package com.coreledger.common.exception;

import com.coreledger.account.domain.exceptions.AccountNotFoundException;
import com.coreledger.account.domain.exceptions.InsufficientFundsException;
import com.coreledger.account.domain.exceptions.InvalidAccountOperationException;
import com.coreledger.transfer.domain.exceptions.InvalidTransferException;
import com.coreledger.transfer.domain.exceptions.TransferNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Global exception handler — maps domain and validation exceptions to
 * consistent HTTP error responses.
 *
 * Uses raw int status codes instead of HttpStatus enum constants —
 * HttpStatus field constants are deprecated in Spring Framework 7.x.
 * ResponseEntity.status(int) accepts int directly and is the correct
 * approach going forward.
 *
 * ErrorResponse is a separate record (own file) — nested types inside
 *
 * @RestControllerAdvice beans cause Spring Framework 7 proxy failures.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFound(AccountNotFoundException ex) {
        log.warn("Account not found: {}", ex.getIdentifier());
        return ResponseEntity.status(404)
                .body(ErrorResponse.of(404, ex.getMessage()));
    }

    @ExceptionHandler(TransferNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTransferNotFound(TransferNotFoundException ex) {
        log.warn("Transfer not found: {}", ex.getIdentifier());
        return ResponseEntity.status(404)
                .body(ErrorResponse.of(404, ex.getMessage()));
    }

    @ExceptionHandler(InvalidTransferException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransfer(InvalidTransferException ex) {
        log.warn("Invalid transfer: {}", ex.getMessage());
        return ResponseEntity.status(422)
                .body(ErrorResponse.of(422, ex.getMessage()));
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFunds(InsufficientFundsException ex) {
        // Log detail internally — never expose balance amounts in the response
        log.warn("Insufficient funds — available: {}, requested: {}",
                ex.getAvailableBalance(), ex.getRequestedAmount());
        return ResponseEntity.status(422)
                .body(ErrorResponse.of(422, "Insufficient funds"));
    }

    @ExceptionHandler(InvalidAccountOperationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOperation(InvalidAccountOperationException ex) {
        log.warn("Invalid account operation: {}", ex.getMessage());
        return ResponseEntity.status(422)
                .body(ErrorResponse.of(422, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationFailures(
            MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();

        log.warn("Validation failed: {}", errors);
        return ResponseEntity.status(400)
                .body(ErrorResponse.ofErrors(400, "Validation failed", errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(500)
                .body(ErrorResponse.of(500, "An unexpected error occurred"));
    }
}
