// app/src/main/java/com/coreledger/config/EventRegistry.java
package com.coreledger.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.coreledger.account.domain.events.AccountCreated;
import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.shared.events.MoneyDeposited;
import com.coreledger.shared.events.MoneyWithdrawn;
import com.coreledger.shared.events.TransferCompleted;
import com.coreledger.shared.events.TransferFailed;
import com.coreledger.shared.events.TransferInitiated;
import com.coreledger.shared.events.TransferReversed;
import com.coreledger.user.domain.events.UserFlagged;

/**
 * Central registry mapping eventType strings to their Java classes.
 *
 * This is the ONLY place you touch when adding a new event type:
 * 1. Create the event class in shared-kernel with @JsonCreator
 * 2. Add one line here
 * Done.
 *
 * Used by KafkaConsumerConfig to deserialize the envelope payload
 * into the correct concrete class.
 */
public class EventRegistry {

    private static final Map<String, Class<? extends DomainEvent>> REGISTRY = new HashMap<>();

    static {
        register("AccountCreated", AccountCreated.class);
        register("MoneyDeposited", MoneyDeposited.class);
        register("MoneyWithdrawn", MoneyWithdrawn.class);
        register("TransferInitiated", TransferInitiated.class);
        register("TransferCompleted", TransferCompleted.class);
        register("TransferFailed", TransferFailed.class);
        register("TransferReversed", TransferReversed.class);
        register("UserFlagged", UserFlagged.class);
    }

    private static void register(String eventType, Class<? extends DomainEvent> clazz) {
        REGISTRY.put(eventType, clazz);
    }

    public static Optional<Class<? extends DomainEvent>> resolve(String eventType) {
        return Optional.ofNullable(REGISTRY.get(eventType));
    }
}