// auth-module/src/main/java/com/coreledger/authentication/domain/model/AuthUser.java
package com.coreledger.authentication.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.crypto.bcrypt.BCrypt;

import com.coreledger.shared.domain.EmailAddress;
import com.coreledger.shared.domain.UserId;

/**
 * Aggregate root representing an authenticated identity in CoreLedger.
 *
 * <p>
 * This is NOT the same as a {@code User} in the user-module. That model owns
 * profile data (name, address, KYC tier). This model owns <em>authentication
 * state</em>:
 * credentials, roles, lock status, and login history. They share the same
 * {@link UserId}
 * as a correlation key, but neither module imports the other's domain model
 * directly.
 *
 * <p>
 * <strong>Auth method design:</strong> CoreLedger supports multiple
 * authentication
 * methods. {@code passwordHash} is only present when
 * {@code authMethod == PASSWORD}.
 * Magic link and TOTP flows populate a {@code TokenPair} through different
 * verification
 * paths but always produce the same output — a valid session. This class models
 * the
 * <em>stored identity</em> regardless of which method is used to verify it.
 *
 * <p>
 * <strong>Immutability:</strong> All fields are final. Behaviours that change
 * state
 * (e.g. recording a failed attempt) return a new {@code AuthUser} instance.
 * This makes
 * state transitions explicit and auditable — you always have a before and
 * after.
 *
 * <p>
 * <strong>Email as identity:</strong> In a banking context, there is no reason
 * for
 * a username that isn't an email address. Email is used as the primary login
 * identifier
 * and is the delivery target for magic links and OTPs. Format validation
 * happens at the
 * boundary (command/request layer), not here — the domain trusts it receives
 * valid input.
 */
public class AuthUser {

    /**
     * Correlates this auth identity to the corresponding User aggregate in
     * user-module.
     * Defined in shared-kernel so neither module depends on the other.
     */
    private final UserId userId;

    /**
     * The user's email address, used as the login identifier.
     * Unique across all AuthUser records.
     */
    private final EmailAddress email;

    /**
     * BCrypt hash of the user's password. Present only when authMethod is PASSWORD.
     * Empty for MAGIC_LINK and TOTP users — they have no password to hash.
     *
     * <p>
     * Never log, serialize to responses, or expose outside the authentication
     * layer.
     */
    private final Optional<String> passwordHash;

    /**
     * The authentication method this user is enrolled in.
     * Determines which verification path is valid at login time.
     */
    private final AuthMethod authMethod;

    /**
     * Granted roles, e.g. {@code ROLE_CUSTOMER}, {@code ROLE_TELLER},
     * {@code ROLE_ADMIN}.
     * Embedded in the JWT at issuance. Evaluated by the authorization layer.
     */
    private final Set<String> roles;

    /**
     * Whether this account is administratively active.
     * False means the account has been disabled by an operator — distinct from a
     * temporary lock caused by failed login attempts.
     */
    private final boolean active;

    /**
     * Number of consecutive failed login attempts since the last successful login.
     * Resets to 0 on successful authentication.
     * When this reaches the configured threshold, {@code lockedUntil} is set.
     */
    private final int failedLoginAttempts;

    /**
     * The time until which this account is temporarily locked due to failed
     * attempts.
     * Empty if the account is not currently locked.
     * Distinct from {@code active} — a locked account can be unlocked automatically
     * when the window passes; an inactive account requires manual operator action.
     */
    private final Optional<Instant> lockedUntil;

    // =========================================================================
    // Constructor (private — use factory methods)
    // =========================================================================

    private AuthUser(
            UserId userId,
            EmailAddress email,
            Optional<String> passwordHash,
            AuthMethod authMethod,
            Set<String> roles,
            boolean active,
            int failedLoginAttempts,
            Instant lockedUntil) {
        this.userId = userId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.authMethod = authMethod;
        // Defensive copy — caller's set mutations don't affect this object
        this.roles = Collections.unmodifiableSet(Set.copyOf(roles));
        this.active = active;
        this.failedLoginAttempts = failedLoginAttempts;
        this.lockedUntil = Optional.ofNullable(lockedUntil);
    }

    // =========================================================================
    // Factory Methods
    // =========================================================================

    /**
     * Creates a new AuthUser for a password-based login.
     *
     * @param userId       correlates to the User aggregate in user-module
     * @param email        login identifier; must be a valid email (validated
     *                     upstream)
     * @param passwordHash BCrypt hash of the raw password; never the raw value
     * @param roles        granted roles; copied defensively
     * @return a new, active, unlocked AuthUser with zero failed attempts
     */
    public static AuthUser createPasswordUser(
            UserId userId,
            EmailAddress email,
            String passwordHash,
            Set<String> roles) {
        return new AuthUser(
                userId,
                email,
                Optional.of(passwordHash),
                AuthMethod.PASSWORD,
                roles,
                true,
                0,
                null);
    }

    /**
     * Creates a new AuthUser for magic link authentication.
     * No password is stored — verification happens via a time-limited signed link.
     *
     * @param userId correlates to the User aggregate in user-module
     * @param email  login identifier and magic link delivery target
     * @param roles  granted roles
     * @return a new, active, unlocked AuthUser with no password hash
     */
    public static AuthUser createMagicLinkUser(
            UserId userId,
            EmailAddress email,
            Set<String> roles) {
        return new AuthUser(
                userId,
                email,
                Optional.empty(),
                AuthMethod.MAGIC_LINK,
                roles,
                true,
                0,
                null);
    }

    /**
     * Reconstitutes an AuthUser from a persistence record.
     * Used by the persistence adapter when loading from the database.
     * Not for creating new users — use the typed factory methods above.
     */
    public static AuthUser reconstitute(
            UserId userId,
            EmailAddress email,
            Optional<String> passwordHash,
            AuthMethod authMethod,
            Set<String> roles,
            boolean active,
            int failedLoginAttempts,
            Instant lockedUntil) {
        return new AuthUser(
                userId,
                email,
                passwordHash,
                authMethod,
                roles,
                active,
                failedLoginAttempts,
                lockedUntil);
    }

    // =========================================================================
    // Behaviours
    // =========================================================================

    /**
     * Returns whether this account is currently locked due to failed login
     * attempts.
     *
     * <p>
     * An account is locked if {@code lockedUntil} is present and that time is still
     * in the future. Once the lock window passes, this returns false automatically
     * —
     * no manual unlock required for time-based locks.
     *
     * <p>
     * This is separate from {@link #isActive()}. A locked account is temporarily
     * blocked; an inactive account is administratively disabled. Both must be
     * checked
     * before allowing login.
     *
     * @return true if the lock window has not yet expired
     */
    public boolean isAccountLocked() {
        return lockedUntil
                .map(lockTime -> Instant.now().isBefore(lockTime))
                .orElse(false);
    }

    /**
     * Returns whether this account is administratively active.
     * An inactive account cannot log in regardless of credentials.
     *
     * @return true if the account has not been disabled by an operator
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Records a failed login attempt and returns the updated AuthUser.
     *
     * <p>
     * Returns a new instance — this object is unchanged. The caller is responsible
     * for persisting the returned instance.
     *
     * <p>
     * If the failed attempt count reaches {@code maxAttempts}, the returned
     * instance
     * will have {@code lockedUntil} set to {@code lockDuration} from now.
     *
     * @param maxAttempts  the threshold at which the account becomes locked
     * @param lockDuration how long to lock the account once the threshold is
     *                     reached
     * @return a new AuthUser reflecting the recorded failure
     */
    public AuthUser recordFailedAttempt(int maxAttempts, java.time.Duration lockDuration) {
        int newCount = this.failedLoginAttempts + 1;
        Instant newLock = (newCount >= maxAttempts)
                ? Instant.now().plus(lockDuration)
                : null;

        return new AuthUser(
                this.userId,
                this.email,
                this.passwordHash,
                this.authMethod,
                this.roles,
                this.active,
                newCount,
                newLock);
    }

    /**
     * Resets failed login attempts after a successful authentication.
     * Returns a new AuthUser with {@code failedLoginAttempts = 0} and no lock.
     *
     * @return a new AuthUser with cleared failure state
     */
    public AuthUser resetFailedAttempts() {
        return new AuthUser(
                this.userId,
                this.email,
                this.passwordHash,
                this.authMethod,
                this.roles,
                this.active,
                0,
                null);
    }

    /**
     * Administratively unlocks a locked account.
     * Used by operators, not by the login flow. Time-based locks expire
     * automatically
     * via {@link #isAccountLocked()} — this is for manual operator intervention.
     *
     * @return a new AuthUser with the lock cleared
     */
    public AuthUser unlock() {
        return new AuthUser(
                this.userId,
                this.email,
                this.passwordHash,
                this.authMethod,
                this.roles,
                this.active,
                0,
                null);
    }

    /**
     * Checks whether this user has been granted the specified role.
     *
     * @param role the role to check, e.g. {@code "ROLE_ADMIN"}
     * @return true if the role is present in this user's role set
     */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean verifyPassword(String rawPassword) {
        if (authMethod != AuthMethod.PASSWORD || passwordHash.isEmpty()) {
            return false;
        }
        return BCrypt.checkpw(rawPassword, passwordHash.get());
    }

    // =========================================================================
    // Getters
    // =========================================================================

    public UserId getUserId() {
        return userId;
    }

    public EmailAddress getEmail() {
        return email;
    }

    public Optional<String> getPasswordHash() {
        return passwordHash;
    }

    public AuthMethod getAuthMethod() {
        return authMethod;
    }

    /** Returns an unmodifiable view of the roles set. */
    public Set<String> getRoles() {
        return roles;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Optional<Instant> getLockedUntil() {
        return lockedUntil;
    }
}
