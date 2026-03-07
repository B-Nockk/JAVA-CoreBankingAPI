// shared-kernel/src/main/java/com/coreledger/shared/events/TransferFailed.java
package com.coreledger.shared.events;

import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.shared.domain.Money;

/**
 * Published by transfer-module when a transfer fails after the source
 * account has already been debited.
 * Consumed by account-module to reverse the debit (re-credit source).
 *
 * Only published when status transitions to FAILED from DEBITED —
 * meaning money has already left the source and must be returned.
 * If failure happens before debit, no reversal event is needed.
 */
public final class TransferFailed extends DomainEvent {

    private final String sourceAccountNumber;
    private final Money amount;
    private final String reason;

    public TransferFailed(
            String transferId,
            String sourceAccountNumber,
            Money amount,
            String reason) {
        super(transferId);
        this.sourceAccountNumber = sourceAccountNumber;
        this.amount = amount;
        this.reason = reason;
    }

    public String getSourceAccountNumber() {
        return sourceAccountNumber;
    }

    public Money getAmount() {
        return amount;
    }

    public String getReason() {
        return reason;
    }
}