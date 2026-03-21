package com.coreledger.authentication.domain.model;

/**
 * Represents the plain-text random string sent to the client.
 * NEVER STORE THIS IN THE DATABASE.
 */
public record RawToken(String value) {
    public RawToken {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("RawToken cannot be empty");
        }
    }
}
