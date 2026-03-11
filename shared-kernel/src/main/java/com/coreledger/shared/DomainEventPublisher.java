// shared-kernel/src/main/java/com/coreledger/shared/DomainEventPublisher.java
package com.coreledger.shared;

import com.coreledger.shared.domain.DomainEvent;

public interface DomainEventPublisher {
    void publishAccountEvent(DomainEvent event);

    void publishTransferEvent(DomainEvent event);
}