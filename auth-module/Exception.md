# Authentication Exception Usage Matrix

This document provides scenarios, context availability, and example log messages
for each exception in the `auth-module`.

---

## Summary Table

| Exception                      | Scenario                                 | Context Available                                      |
| ------------------------------ | ---------------------------------------- | ------------------------------------------------------ |
| InvalidCredentialsException    | Wrong username or password               | userEmail ✅, userId ❌                                |
| TokenExpiredException          | JWT or refresh token past expiry         | userId ✅, userEmail ✅, expiredAt ✅                  |
| TokenRevokedException          | Refresh token revoked (logout, rotation) | userId ✅, userEmail ✅                                |
| InvalidSignatureException      | HMAC/JWT signature mismatch              | algorithm ✅, userId ❌, userEmail ❌                  |
| ReplayAttackException          | Nonce reused or timestamp outside window | nonce ✅, requestTimestamp ✅, userId ❌, userEmail ❌ |
| TokenReuseDetectedException    | Refresh token reused after rotation      | userId ✅, tokenId ✅                                  |
| UserAlreadyExistsException     | Signup attempted with existing account   | userEmail ✅, userId ❌                                |
| AccountLockedException         | Too many failed attempts / admin lockout | userId ✅, userEmail ✅                                |
| WeakPasswordException          | Signup with weak password                | userEmail ✅, userId ❌                                |
| MfaRequiredException           | MFA required but not provided            | userId ✅, userEmail ✅                                |
| MfaFailedException             | MFA challenge failed                     | userId ✅, userEmail ✅                                |
| UnsupportedAuthMethodException | Client attempted unsupported auth method | authMethod ✅, userId ❌, userEmail ❌                 |

---

## Detailed Scenarios and Example Logs

## InvalidCredentialsException

**Scenario:** Wrong username or password (never reveal which).
**Context available:**

- `userId`: ❌ (unknown before lookup)
- `userEmail`: ✅ (attempted email/username)

**Example log:**

> Authentication failed - correlationId: 123e..., userId: [unknown], userEmail: <user@example.com>, ip: 192.168.1.10, timestamp: 2026-03-23T08:28:00Z, reason: Password mismatch

---

## TokenExpiredException

**Scenario:** JWT or refresh token past expiry.
**Context available:**

- `userId`: ✅ (parsed from token)
- `userEmail`: ✅ (parsed from token)
- `expiredAt`: ✅

**Example log:**

> Token expired - correlationId: 456e..., userId: 42, userEmail: <user@example.com>, ip: 192.168.1.10, expiredAt: 2026-03-22T23:59:59Z, timestamp: 2026-03-23T08:28:00Z, reason: Access token expired

---

## TokenRevokedException

**Scenario:** Refresh token revoked (logout, rotation).
**Context available:**

- `userId`: ✅
- `userEmail`: ✅

**Example log:**

> Token revoked - correlationId: 789e..., userId: 42, userEmail: <user@example.com>, ip: 192.168.1.10, timestamp: 2026-03-23T08:28:00Z, reason: User logout

---

## InvalidSignatureException

**Scenario:** HMAC/JWT signature mismatch.
**Context available:**

- `userId`: ❌ (token invalid before parsing)
- `userEmail`: ❌
- `algorithm`: ✅

**Example log:**

> Invalid signature - correlationId: abc..., algorithm: HS256, ip: 192.168.1.10, timestamp: 2026-03-23T08:28:00Z, reason: Signature mismatch

---

## ReplayAttackException

**Scenario:** Request replay detected (nonce reused, timestamp outside window).
**Context available:**

- `userId`: ❌ (attack may occur before user resolution)
- `userEmail`: ❌
- `nonce`: ✅
- `requestTimestamp`: ✅

**Example log:**

> Replay attack - correlationId: def..., userId: [unknown], userEmail: [unknown], ip: 192.168.1.10, nonce: abc123, requestTimestamp: 2026-03-23T08:20:00Z, timestamp: 2026-03-23T08:28:00Z, reason: Nonce already used

---

## TokenReuseDetectedException

**Scenario:** Refresh token reused after rotation.
**Context available:**

- `userId`: ✅
- `tokenId`: ✅

**Example log:**

> Token reuse detected - correlationId: ghi..., userId: 42, tokenId: 123e4567-e89b-12d3-a456-426614174000, timestamp: 2026-03-23T08:28:00Z

---

## UserAlreadyExistsException

**Scenario:** Signup attempted with existing email/username.
**Context available:**

- `userId`: ❌
- `userEmail`: ✅

**Example log:**

> Signup failed - correlationId: jkl..., userId: [unknown], userEmail: <user@example.com>, ip: 192.168.1.10, timestamp: 2026-03-23T08:28:00Z, reason: Email already registered

---

## AccountLockedException

**Scenario:** Too many failed attempts, admin lockout.
**Context available:**

- `userId`: ✅
- `userEmail`: ✅

**Example log:**

> Account locked - correlationId: mno..., userId: 42, userEmail: <user@example.com>, ip: 192.168.1.10, timestamp: 2026-03-23T08:28:00Z, reason: Too many failed attempts

---

## WeakPasswordException

**Scenario:** Signup with weak password.
**Context available:**

- `userId`: ❌
- `userEmail`: ✅

**Example log:**

> Weak password - correlationId: pqr..., userId: [unknown], userEmail: <user@example.com>, ip: 192.168.1.10, timestamp: 2026-03-23T08:28:00Z, reason: Password does not meet policy

---

## MfaRequiredException

**Scenario:** MFA required but not provided.
**Context available:**

- `userId`: ✅
- `userEmail`: ✅

**Example log:**

> MFA required - correlationId: stu..., userId: 42, userEmail: <user@example.com>, ip: 192.168.1.10, timestamp: 2026-03-23T08:28:00Z, reason: MFA challenge missing

---

## MfaFailedException

**Scenario:** MFA challenge failed.
**Context available:**

- `userId`: ✅
- `userEmail`: ✅

**Example log:**

> MFA failed - correlationId: vwx..., userId: 42, userEmail: <user@example.com>, ip: 192.168.1.10, timestamp: 2026-03-23T08:28:00Z, reason: Invalid OTP

---

## UnsupportedAuthMethodException

**Scenario:** Client attempted unsupported auth method.
**Context available:**

- `userId`: ❌
- `userEmail`: ❌
- `authMethod`: ✅

**Example log:**

> Unsupported auth method - correlationId: yz..., userId: [unknown], userEmail: [unknown], ip: 192.168.1.10, timestamp: 2026-03-23T08:28:00Z, reason: Method 'TwitterOAuth' not enabled
