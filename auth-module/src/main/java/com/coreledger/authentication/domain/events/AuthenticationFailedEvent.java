// auth-module/src/main/java/com/coreledger/authentication/domain/events/AuthenticationFailedEvent.java
package com.coreledger.authentication.domain.events;

import java.time.Instant;

import com.coreledger.authentication.domain.model.AuthFailureReason;
import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.shared.domain.EmailAddress;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@Getter
public class AuthenticationFailedEvent extends DomainEvent {
    private final EmailAddress userEmail;
    private final AuthFailureReason reason; // e.g. INVALID_CREDENTIALS, ACCOUNT_LOCKED, TOKEN_EXPIRED
    private final Instant attemptedAt;
    private final String ipAddress;
    private final String userAgent;

    public AuthenticationFailedEvent(EmailAddress userEmail,
            AuthFailureReason reason,
            Instant attemptedAt,
            String ipAddress,
            String userAgent) {
        super(userEmail.toString());
        this.userEmail = userEmail;
        this.reason = reason;
        this.attemptedAt = attemptedAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    private AuthenticationFailedEvent(
            String aggregateId,
            String eventId,
            Instant occurredOn,
            EmailAddress userEmail,
            AuthFailureReason reason,
            Instant attemptedAt,
            String ipAddress,
            String userAgent) {
        super(aggregateId, eventId, occurredOn);
        this.userEmail = userEmail;
        this.reason = reason;
        this.attemptedAt = attemptedAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    @JsonCreator
    static AuthenticationFailedEvent restore(
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") Instant occurredOn,
            // @JsonProperty("userEmail") EmailAddress userEmail,
            @JsonProperty("reason") String reason,
            @JsonProperty("attemptedAt") Instant attemptedAt,
            @JsonProperty("ipAddress") String ipAddress,
            @JsonProperty("userAgent") String userAgent) {
        EmailAddress userEmail = EmailAddress.of(aggregateId);
        return new AuthenticationFailedEvent(
                aggregateId,
                eventId,
                occurredOn,
                userEmail,
                AuthFailureReason.valueOf(reason),
                attemptedAt,
                ipAddress,
                userAgent);
    }
}
