// account-module/src/main/java/com/coreledger/account/domain/exception/AccountNotFoundException.java
package com.coreledger.account.domain.exceptions;

/**
 * Thrown when an account lookup returns no result.
 *
 * This is a domain exception — it lives in the domain layer because
 * "account not found" is a domain concept, not an infrastructure concern.
 * The web adapter will catch this and map it to HTTP 404.
 *
 * RuntimeException (unchecked) by design — callers are not forced to
 * handle it at every call site. The global exception handler catches it.
 */
public class AccountNotFoundException extends RuntimeException {

    private final String identifier;

    public AccountNotFoundException(String identifier) {
        super("Account not found: " + identifier);
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}