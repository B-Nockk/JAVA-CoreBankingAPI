// transfer-module/src/main/java/com/coreledger/transfer/domain/TransferStatus.java
package com.coreledger.transfer.domain.model;

/**
 * Lifecycle states of a Transfer.
 *
 * The full state machine:
 *
 * INITIATED → DEBITED → COMPLETED
 * │ │
 * │ └──→ FAILED → REVERSED
 * │
 * └──→ FAILED
 *
 * INITIATED — transfer record created, no money moved yet
 * DEBITED — source account debited successfully, credit pending
 * COMPLETED — destination account credited, transfer fully settled
 * FAILED — something went wrong, money not fully moved
 * REVERSED — source account re-credited after a failed debit
 *
 * Why DEBITED is its own state:
 * Between the debit and the credit there is a window where money has
 * left the source but not yet arrived at the destination. This is real
 * in banking — it's called "float." Tracking this state explicitly means
 * we can identify and resolve transfers stuck in this window.
 * Without this state, a failure after debit but before credit would be
 * invisible until someone noticed the balance discrepancy.
 */
public enum TransferStatus {
    INITIATED,
    DEBITED,
    COMPLETED,
    FAILED,
    REVERSED
}