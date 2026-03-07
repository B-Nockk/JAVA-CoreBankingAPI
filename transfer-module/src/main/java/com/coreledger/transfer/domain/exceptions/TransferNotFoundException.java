// transfer-module/src/main/java/com/coreledger/transfer/domain/exceptions/TransferNotFoundException.java
package com.coreledger.transfer.domain.exceptions;

/**
 * Thrown when a transfer lookup returns no result.
 * Maps to HTTP 404 at the web adapter boundary.
 */
public class TransferNotFoundException extends RuntimeException {

    private final String identifier;

    public TransferNotFoundException(String identifier) {
        super("Transfer not found: " + identifier);
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}