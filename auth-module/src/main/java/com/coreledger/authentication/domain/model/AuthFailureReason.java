// auth-module/src/main/java/com/coreledger/authentication/domain/model/AuthFailureReason.java
package com.coreledger.authentication.domain.model;

public enum AuthFailureReason {
    INVALID_CREDENTIALS,
    ACCOUNT_LOCKED,
    TOKEN_EXPIRED,
    TOKEN_REVOKED,
    TOKEN_REUSE_DETECTED,
    INVALID_SIGNATURE,
    REPLAY_ATTACK,
    MFA_REQUIRED,
    MFA_FAILED,
    UNSUPPORTED_AUTH_METHOD,
    WEAK_PASSWORD,
    USER_NOT_FOUND,
    USER_ALREADY_EXISTS
}
