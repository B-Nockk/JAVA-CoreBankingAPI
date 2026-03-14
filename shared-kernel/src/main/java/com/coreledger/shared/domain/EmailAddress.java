package com.coreledger.shared.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Email address value object with built-in validation.
 * Use this instead of String for stronger type safety.
 *
 * <p>
 * Usage:
 *
 * <pre>
 * public record UserDetails(
 *     EmailAddress email,  // Strongly typed!
 *     // ... other fields
 * ) {}
 * </pre>
 */
public final class EmailAddress {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final int MAX_LENGTH = 254;
    private final String value;

    private static final Set<String> DISPOSABLE_DOMAINS = Set.of(
            "mailinator.com",
            "10minutemail.com",
            "guerrillamail.com",
            "trashmail.com",
            "tempmail.com",
            "yopmail.com",
            "getnada.com");

    private static final Set<String> VALID_DOMAINS = Set.of(
            "gmail.com",
            "outlook.com",
            "yahoo.com",
            "protonmail.com",
            "icloud.com"
    // add corporate domains here if needed
    );

    enum Mode {
        BLACKLIST, WHITELIST
    }

    private EmailAddress(String value) {
        this.value = normalize(value);
    }

    /**
     * Factory method to create an EmailAddress.
     *
     * @param email the raw email string
     * @return validated EmailAddress
     * @throws IllegalArgumentException if email is invalid
     */
    @JsonCreator
    public static EmailAddress of(String email) {
        if (email == null) {
            throw new IllegalArgumentException("email cannot be null");
        }

        // if (email == null || email.isBlank()) {
        // throw new IllegalArgumentException("Email cannot be null or blank");
        // }

        String normalized = normalize(email);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("email cannot be empty");
        }

        if (normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("email exceeds max length %d: %s", MAX_LENGTH, email));
        }

        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }

        // Layer 5: Additional business rules (optional)
        // Throw if the domain is NOT allowed under the chosen mode
        if (!isDomainAllowed(normalized, Mode.BLACKLIST)) {
            throw new IllegalArgumentException("Disposable email not allowed: " + email);
        }

        return new EmailAddress(normalized);
    }

    /**
     * Creates an EmailAddress without validation (for deserialization).
     * Use with caution - only for frameworks.
     */
    public static EmailAddress unsafe(String email) {
        return new EmailAddress(email);
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase();
    }

    @JsonValue
    public String value() {
        return value;
    }

    public String getDomain() {
        return value.substring(value.indexOf('@') + 1);
    }

    public String getLocalPart() {
        return value.substring(0, value.indexOf('@'));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        EmailAddress that = (EmailAddress) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }

    /**
     * Checks if the email domain is allowed.
     * Mode can be "BLACKLIST" or "WHITELIST".
     */
    private static boolean isDomainAllowed(String email, Mode mode) {
        String domain = email.substring(email.indexOf('@') + 1);

        return switch (mode) {
            case BLACKLIST -> !DISPOSABLE_DOMAINS.contains(domain);
            case WHITELIST -> VALID_DOMAINS.contains(domain);
        };
    }
}
