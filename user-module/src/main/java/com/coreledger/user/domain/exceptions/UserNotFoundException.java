// user-module/src/main/java/com/coreledger/user/domain/exceptions/UserNotFoundException.java
package com.coreledger.user.domain.exceptions;

/**
 * Thrown when a user lookup returns no result.
 *
 * This is a domain exception — it lives in the domain layer because
 * "user not found" is a domain concept, not an infrastructure concern.
 * The web adapter will catch this and map it to HTTP 404.
 *
 * RuntimeException (unchecked) by design — callers are not forced to
 * handle it at every call site. The global exception handler catches it.
 */
public class UserNotFoundException extends RuntimeException {

    private final String identifier;

    public UserNotFoundException(String identifier) {
        super("Account not found: " + identifier);
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}
