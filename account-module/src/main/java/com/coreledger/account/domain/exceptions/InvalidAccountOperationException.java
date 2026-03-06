// account-module/src/main/java/com/coreledger/account/domain/exceptions/InvalidAccountOperationException.java
package com.coreledger.account.domain.exceptions;

/**
 * Thrown when an operation is attempted that violates the account's
 * current state or business rules — other than insufficient funds.
 *
 * Examples:
 * - Debiting a FROZEN account
 * - Any operation on a CLOSED account
 * - Attempting to close an account with a positive balance
 * - Currency mismatch on a transaction
 *
 * This is intentionally broad — it is the catch-all for domain rule
 * violations that don't warrant their own specific exception type yet.
 * As the domain grows, specific cases can be extracted into their own
 * exception classes (e.g. AccountFrozenException, CurrencyMismatchException).
 */
public class InvalidAccountOperationException extends RuntimeException {

    public InvalidAccountOperationException(String message) {
        super(message);
    }

    public InvalidAccountOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}