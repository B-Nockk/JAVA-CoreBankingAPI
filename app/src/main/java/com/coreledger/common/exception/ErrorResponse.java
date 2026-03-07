// app/src/main/java/com/coreledger/common/exception/ErrorResponse.java
package com.coreledger.common.exception;

import java.time.Instant;
import java.util.List;

/**
 * Consistent error response envelope for all API errors.
 *
 * Extracted from GlobalExceptionHandler into its own file to avoid
 * Spring Framework 7 proxy issues with nested types in @RestControllerAdvice
 * beans.
 */
public record ErrorResponse(
        int status,
        String message,
        Instant timestamp,
        List<String> errors) {

    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(status, message, Instant.now(), List.of());
    }

    public static ErrorResponse ofErrors(int status, String message, List<String> errors) {
        return new ErrorResponse(status, message, Instant.now(), errors);
    }
}