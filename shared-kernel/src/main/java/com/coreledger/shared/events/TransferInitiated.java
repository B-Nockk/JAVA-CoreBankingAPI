// shared-kernel/src/main/java/com/coreledger/shared/events/TransferInitiated.java
package com.coreledger.shared.events;

import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.shared.domain.Money;

/**
 * Published by transfer-module when a transfer is initiated.
 * Consumed by account-module to debit the source account.
 *
 * Carries everything the account-module needs to execute the debit
 * without querying back — self-contained fact.
 */
public final class TransferInitiated extends DomainEvent {

    private final String sourceAccountNumber;
    private final String destinationAccountNumber;
    private final Money amount;

    public TransferInitiated(
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