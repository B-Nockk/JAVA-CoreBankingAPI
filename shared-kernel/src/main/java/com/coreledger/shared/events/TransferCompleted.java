// shared-kernel/src/main/java/com/coreledger/shared/events/TransferCompleted.java
package com.coreledger.shared.events;

import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.shared.domain.Money;

/**
 * Published by transfer-module when a transfer fully settles.
 * Consumed by notification-module (future) to alert both parties.
 */
public final class TransferCompleted extends DomainEvent {

    private final String sourceAccountNumber;
    private final String destinationAccountNumber;
    private final Money amount;

    public TransferCompleted(
            String transferId,
            String sourceAccountNumber,
            String destinationAccountNumber,
            Money amount) {
        super(transferId);
        this.sourceAccountNumber = sourceAccountNumber;
        this.destinationAccountNumber = destinationAccountNumber;
        this.amount = amount;
    }

    public String getSourceAccountNumber() {
        return sourceAccountNumber;
    }

    public String getDestinationAccountNumber() {
        return destinationAccountNumber;
    }

    public Money getAmount() {
        return amount;
    }
}