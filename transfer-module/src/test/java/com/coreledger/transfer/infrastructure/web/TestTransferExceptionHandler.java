// transfer-module/src/test/java/com/coreledger/transfer/infrastructure/web/TestTransferExceptionHandler.java
package com.coreledger.transfer.infrastructure.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.coreledger.transfer.domain.exceptions.InvalidTransferException;
import com.coreledger.transfer.domain.exceptions.TransferNotFoundException;

/**
 * Test-only exception handler for @WebMvcTest in transfer-module.
 * Mirrors the relevant handlers from GlobalExceptionHandler in app/
 * without importing account-module or other cross-module types.
 */
@RestControllerAdvice
public class TestTransferExceptionHandler {

    @ExceptionHandler(TransferNotFoundException.class)
    public ResponseEntity<Void> handleNotFound(TransferNotFoundException ex) {
        return ResponseEntity.status(404).build();
    }

    @ExceptionHandler(InvalidTransferException.class)
    public ResponseEntity<Void> handleInvalidTransfer(InvalidTransferException ex) {
        return ResponseEntity.status(422).build();
    }
}