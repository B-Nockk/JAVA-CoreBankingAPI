package com.coreledger.account.infrastructure.web;

import com.coreledger.account.domain.exceptions.AccountNotFoundException;
import com.coreledger.account.domain.exceptions.InsufficientFundsException;
import com.coreledger.account.domain.exceptions.InvalidAccountOperationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Test-only exception handler for @WebMvcTest in account-module.
 * The real GlobalExceptionHandler lives in app/ and can't be loaded here
 * because it references transfer-module exception types.
 * This handler covers only the exceptions AccountController can throw.
 */
@RestControllerAdvice
public class TestExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<Void> handleNotFound(AccountNotFoundException ex) {
        return ResponseEntity.status(404).build();
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<Void> handleInsufficientFunds(InsufficientFundsException ex) {
        return ResponseEntity.status(422).build();
    }

    @ExceptionHandler(InvalidAccountOperationException.class)
    public ResponseEntity<Void> handleInvalidOperation(InvalidAccountOperationException ex) {
        return ResponseEntity.status(422).build();
    }
}