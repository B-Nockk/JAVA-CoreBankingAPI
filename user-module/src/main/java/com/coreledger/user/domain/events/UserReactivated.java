// user-module/src/main/java/com/coreledger/user/domain/events/UserReactivated.java
package com.coreledger.user.domain.events;

import java.time.Instant;

import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.shared.domain.UserId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class UserReactivated extends DomainEvent {

    private final String reason;

    public UserReactivated(
            UserId userId,
            String reason) {
        super(userId.toString());
        this.reason = reason;
    }

    private UserReactivated(
            String aggregateId,
            String eventId,
            Instant occurredOn,
            String reason) {
        super(aggregateId, eventId, occurredOn);
        this.reason = reason;
    }

    @JsonCreator
    static UserReactivated restore(
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") Instant occurredOn,
            @JsonProperty("reason") String reason) {
        return new UserReactivated(
                aggregateId,
                eventId,
                occurredOn,
                reason);
    }

    @JsonProperty
    public String getReason() {
        return reason;
    }

    public UserId getUserId() {
        return UserId.of(getAggregateId());
    }
}