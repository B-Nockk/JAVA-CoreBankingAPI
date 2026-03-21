package com.coreledger.authentication.domain.model;

/**
 * Represents the SHA-256 hashed version of the token.
 * THIS IS WHAT GETS STORED IN THE DATABASE.
 */
public record TokenHash(String value) {
    public TokenHash {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TokenHash cannot be empty");
        }
    }
}