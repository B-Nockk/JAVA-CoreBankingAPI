// user-module/src/main/java/com/coreledger/user/domain/events/UserDeactivated.java
package com.coreledger.user.domain.events;

import java.time.Instant;

import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.user.domain.model.UserId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Domain event raised when a user is deactivated.
 * This is distinct from suspension or flagging,
 * and indicates the user is no longer active in the system.
 *
 * <p>
 * Example usage:
 *
 * <pre>
 * User.deactivate("customer requested closure") → raises UserDeactivated.
 * </pre>
 *
 * <p>
 * Consumers (account module, reporting, fraud monitoring) subscribe to
 * UserDeactivated events. They don't care about the internal user state enum;
 * they only need to know that a user was deactivated and the reason why.
 */
public final class UserDeactivated extends DomainEvent {

    private final String reason;

    public UserDeactivated(
            UserId userId,
            String reason) {
        super(userId.toString());
        this.reason = reason;
    }

    private UserDeactivated(
            String aggregateId,
            String eventId,
            Instant occurredOn,
            String reason) {
        super(aggregateId, eventId, occurredOn);
        this.reason = reason;
    }

    @JsonCreator
    static UserDeactivated restore(
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") Instant occurredOn,
            @JsonProperty("reason") String reason) {
        return new UserDeactivated(
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
