// transfer-module/src/main/java/com/coreledger/transfer/domain/exceptions/InvalidTransferException.java
package com.coreledger.transfer.domain.exceptions;

/**
 * Thrown when a transfer request violates business rules.
 *
 * Examples:
 * - Source and destination accounts are the same
 * - Source account does not exist or is not ACTIVE
 * - Destination account does not exist or is CLOSED
 * - Amount is zero or negative
 * - Insufficient funds in source account
 *
 * Maps to HTTP 422 at the web adapter boundary.
 */
public class InvalidTransferException extends RuntimeException {

    public InvalidTransferException(String message) {
        super(message);
    }

    public InvalidTransferException(String message, Throwable cause) {
        super(message, cause);
    }
}