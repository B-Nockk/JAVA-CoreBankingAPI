# Authentication Domain Event Matrix

This document outlines the key authentication events, when they fire, what fields they carry, and example payloads/logs.

---

## Summary Table

| Event                     | Scenario                                | Key Fields Available                                                                |
| ------------------------- | --------------------------------------- | ----------------------------------------------------------------------------------- |
| UserAuthenticatedEvent    | Successful login / signup               | userId ✅, userEmail ✅, issuedAt ✅, ipAddress ✅, userAgent ✅                    |
| TokenRefreshedEvent       | Token refresh succeeds                  | userId ✅, oldTokenId ✅, newTokenId ✅, refreshedAt ✅, ipAddress ✅, userAgent ✅ |
| AuthenticationFailedEvent | Any failed authentication attempt       | username ✅, reason ✅, attemptedAt ✅, ipAddress ✅, userAgent ✅                  |
| UserSignedUpEvent         | New user registration                   | userId ✅, userEmail ✅, signedUpAt ✅, ipAddress ✅, userAgent ✅                  |
| UserLoggedOutEvent        | Explicit logout / refresh token revoked | userId ✅, userEmail ✅, loggedOutAt ✅, ipAddress ✅, userAgent ✅                 |
| MfaChallengedEvent        | MFA challenge issued                    | userId ✅, challengeId ✅, challengedAt ✅, ipAddress ✅, userAgent ✅              |
| MfaSucceededEvent         | MFA challenge passed                    | userId ✅, challengeId ✅, succeededAt ✅, ipAddress ✅, userAgent ✅               |
| MfaFailedEvent            | MFA challenge failed                    | userId ✅, challengeId ✅, failedAt ✅, ipAddress ✅, userAgent ✅                  |
| PasswordChangedEvent      | User changed password                   | userId ✅, userEmail ✅, changedAt ✅, ipAddress ✅, userAgent ✅                   |

---

## Example Payloads

### UserAuthenticatedEvent

```json
{
  "aggregateId": "user-123",
  "eventId": "evt-456",
  "occurredOn": "2026-03-23T09:45:00Z",
  "userId": "user-123",
  "userEmailAddress": "user@example.com",
  "issuedAt": "2026-03-23T09:45:00Z",
  "ipAddress": "192.168.1.10",
  "userAgent": "Mozilla/5.0"
}
```

### TokenRefreshedEvent

```json
{
  "aggregateId": "user-123",
  "eventId": "evt-789",
  "occurredOn": "2026-03-23T09:46:00Z",
  "oldTokenId": "tok-111",
  "newTokenId": "tok-222",
  "refreshedAt": "2026-03-23T09:46:00Z",
  "ipAddress": "192.168.1.10",
  "userAgent": "Mozilla/5.0"
}
```

### AuthenticationFailedEvent

```json
{
  "aggregateId": "user@example.com",
  "eventId": "evt-999",
  "occurredOn": "2026-03-23T09:47:00Z",
  "username": "user@example.com",
  "reason": "INVALID_CREDENTIALS",
  "attemptedAt": "2026-03-23T09:47:00Z",
  "ipAddress": "192.168.1.10",
  "userAgent": "Mozilla/5.0"
}
```

- _(Other events follow the same pattern.)_
