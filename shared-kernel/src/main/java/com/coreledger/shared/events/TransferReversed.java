// shared-kernel/src/main/java/com/coreledger/shared/events/TransferReversed.java
package com.coreledger.shared.events;

import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.shared.domain.Money;

/**
 * Published by account-module when a source account reversal completes.
 * Consumed by transfer-module to mark the transfer REVERSED.
 *
 * At this point the money is back where it started and the transfer
 * is in its terminal failure state.
 */
public final class TransferReversed extends DomainEvent {

    private final String sourceAccountNumber;
    private final Money amount;

    public TransferReversed(
            String transferId,
            String sourceAccountNumber,
            Money amount) {
        super(transferId);
        this.sourceAccountNumber = sourceAccountNumber;
        this.amount = amount;
    }

    public String getSourceAccountNumber() {
        return sourceAccountNumber;
    }

    public Money getAmount() {
        return amount;
    }
}