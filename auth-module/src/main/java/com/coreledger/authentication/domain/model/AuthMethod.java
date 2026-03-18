package com.coreledger.authentication.domain.model;

/**
 * The authentication method a user is enrolled in.
 *
 * <p>
 * This determines which verification path is valid at login time and
 * which fields on {@link AuthUser} are expected to be populated.
 *
 * <ul>
 * <li>{@code PASSWORD} — user verifies with email + password.
 * {@code passwordHash} on {@code AuthUser} is always present.</li>
 * <li>{@code MAGIC_LINK} — user receives a time-limited signed link via email.
 * No password is stored. The link itself is the credential.</li>
 * <li>{@code TOTP} — user verifies with a time-based one-time password from
 * an authenticator app (e.g. Google Authenticator). Typically layered on
 * top of PASSWORD as a second factor rather than used standalone.</li>
 * </ul>
 *
 * <p>
 * All three methods produce the same output upon successful verification:
 * a {@link TokenPair} containing an access token and a refresh token.
 * The auth method determines the path to get there, not the destination.
 */
public enum AuthMethod {

    /**
     * Email + password credential verification.
     * {@code AuthUser.passwordHash} is present when this method is active.
     */
    PASSWORD,

    /**
     * Passwordless login via a signed, time-limited link sent to the user's email.
     * {@code AuthUser.passwordHash} is empty for magic link users.
     *
     * <p>
     * The link contains a signed token (HMAC or JWT) that expires in a short window
     * (typically 10–15 minutes). The authentication service verifies the signature
     * and
     * expiry before issuing a full session TokenPair.
     */
    MAGIC_LINK,

    /**
     * Time-based one-time password via an authenticator app (RFC 6238 / TOTP).
     * Typically used as a second factor alongside PASSWORD.
     *
     * <p>
     * The shared TOTP secret is stored encrypted in the user's auth record,
     * not in {@code passwordHash}. Implementation of TOTP is a future extension.
     */
    TOTP
}