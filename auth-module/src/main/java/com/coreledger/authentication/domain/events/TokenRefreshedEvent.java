// auth-module/src/main/java/com/coreledger/authentication/domain/events/TokenRefreshedEvent.java
package com.coreledger.authentication.domain.events;

import java.time.Instant;

import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.shared.domain.UserId;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;

@Getter
public class TokenRefreshedEvent extends DomainEvent {
    private final UserId userid;
    private final String oldTokenId;
    private final String newTokenId;
    private final Instant refreshedAt;
    private final String ipAddress;
    private final String userAgent;

    public TokenRefreshedEvent(UserId userId,
            String oldTokenId,
            String newTokenId,
            Instant refreshedAt,
            String ipAddress,
            String userAgent) {
        super(userId.toString());
        this.userid = userId;
        this.oldTokenId = oldTokenId;
        this.newTokenId = newTokenId;
        this.refreshedAt = refreshedAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    private TokenRefreshedEvent(
            String aggregateId,
            String eventId,
            Instant occurredOn,
            UserId userId,
            String oldTokenId,
            String newTokenId,
            Instant refreshedAt,
            String ipAddress,
            String userAgent) {
        super(aggregateId, eventId, occurredOn);
        this.userid = userId;
        this.oldTokenId = oldTokenId;
        this.newTokenId = newTokenId;
        this.refreshedAt = refreshedAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    @JsonCreator
    static TokenRefreshedEvent restore(
            String aggregateId,
            String eventId,
            Instant occurredOn,
            String oldTokenId,
            String newTokenId,
            Instant refreshedAt,
            String ipAddress,
            String userAgent) {

        UserId userId = UserId.of(aggregateId);
        return new TokenRefreshedEvent(
                aggregateId,
                eventId,
                occurredOn,
                userId,
                oldTokenId,
                newTokenId,
                refreshedAt,
                ipAddress,
                userAgent);
    }
}
