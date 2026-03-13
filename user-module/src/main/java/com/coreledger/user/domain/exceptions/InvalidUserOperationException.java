// user-module/src/main/java/com/coreledger/user/domain/exceptions/InvalidUserOperationException.java
package com.coreledger.user.domain.exceptions;

public class InvalidUserOperationException extends RuntimeException {
    public InvalidUserOperationException(String message) {
        super(message);
    }

    public InvalidUserOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
