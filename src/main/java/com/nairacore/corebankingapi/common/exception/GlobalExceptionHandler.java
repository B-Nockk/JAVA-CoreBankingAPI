// src/main/java/com/nairacore/corebankingapi/common/exception/GlobalExceptionHandler.java
package com.nairacore.corebankingapi.common.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// This annotation tells Spring to intercept exceptions thrown by ANY controller
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Define a standard shape for all our API errors
    public record ErrorResponse(
            LocalDateTime timestamp,
            int status,
            String error,
            String message) {
    }

    // 2. Catch Business Rule Violations from our Domain Model
    @ExceptionHandler({ IllegalStateException.class, IllegalArgumentException.class })
    public ResponseEntity<ErrorResponse> handleDomainExceptions(RuntimeException ex) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage() // E.g., "Insufficient funds: Minimum balance of 1000 NGN..."
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // 3. Catch @Valid DTO Validation Errors (e.g., negative initial deposit)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        // Extract the specific field that failed and its error message
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }
}