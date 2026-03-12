// shared-kernel/src/main/java/com/coreledger/shared/kafka/EventDeserializer.java
package com.coreledger.shared.kafka;

import com.coreledger.shared.domain.DomainEvent;

public interface EventDeserializer {
    DomainEvent deserialize(EventEnvelope envelope);
}