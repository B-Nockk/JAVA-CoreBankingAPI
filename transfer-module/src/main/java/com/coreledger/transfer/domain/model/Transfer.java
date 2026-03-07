// transfer-module/src/main/java/com/coreledger/transfer/domain/Transfer.java
package com.coreledger.transfer.domain.model;

import java.util.Objects;

import com.coreledger.shared.domain.AuditMetadata;
import com.coreledger.shared.domain.Money;

/**
 * Aggregate Root for the Transfer bounded context.
 *
 * A Transfer is an orchestration record — it tracks the lifecycle of
 * moving money from one account to another. It does not hold Money itself;
 * it records the intent, the amount, and the current state of execution.
 *
 * Key design decisions:
 *
 * 1. Transfer owns its own state machine.
 * State transitions are methods on this class, not external setters.
 * Invalid transitions throw immediately — the aggregate protects itself.
 *
 * 2. sourceAccountNumber / destinationAccountNumber, not AccountId.
 * Transfer-module has no dependency on account-module. It knows accounts
 * by their public-facing account number (a string), not by their internal
 * domain identity. This is the anti-corruption layer in practice.
 *
 * 3. No Transaction children here.
 * The account ledger entries (Transaction objects) live in account-module.
 * Transfer only records the orchestration — what was attempted and what
 * happened. The actual money movement is account-module's responsibility.
 *
 * 4. failureReason is nullable.
 * Only populated when status is FAILED or REVERSED.
 */
public class Transfer {

    private final TransferId id;
    private final String sourceAccountNumber;
    private final String destinationAccountNumber;
    private final Money amount;
    private TransferStatus status;
    private String failureReason;
    private final AuditMetadata audit;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    private Transfer(
            TransferId id,
            String sourceAccountNumber,
            String destinationAccountNumber,
            Money amount,
            AuditMetadata audit) {
        this.id = Objects.requireNonNull(id);
        this.sourceAccountNumber = Objects.requireNonNull(sourceAccountNumber);
        this.destinationAccountNumber = Objects.requireNonNull(destinationAccountNumber);
        this.amount = Objects.requireNonNull(amount);
        this.status = TransferStatus.INITIATED;
        this.failureReason = null;
        this.audit = Objects.requireNonNull(audit);

        validateAccounts();
        validateAmount();
    }

    public static Transfer initiate(
            String sourceAccountNumber,
            String destinationAccountNumber,
            Money amount,
            String initiatedBy) {
        return new Transfer(
                TransferId.generate(),
                sourceAccountNumber,
                destinationAccountNumber,
                amount,
                AuditMetadata.now(initiatedBy));
    }

    public static Transfer reconstitute(
            TransferId id,
            String sourceAccountNumber,
            String destinationAccountNumber,
            Money amount,
            TransferStatus status,
            String failureReason,
            AuditMetadata audit) {
        Transfer transfer = new Transfer(id, sourceAccountNumber,
                destinationAccountNumber, amount, audit);
        transfer.status = status;
        transfer.failureReason = failureReason;
        return transfer;
    }

    // -------------------------------------------------------------------------
    // State transitions — each validates the current state before moving
    // -------------------------------------------------------------------------

    /**
     * Called when the source account has been successfully debited.
     * Money has left the source — credit must now follow.
     */
    public void markDebited() {
        requireStatus(TransferStatus.INITIATED);
        this.status = TransferStatus.DEBITED;
    }

    /**
     * Called when the destination account has been successfully credited.
     * Transfer is fully settled.
     */
    public void markCompleted() {
        requireStatus(TransferStatus.DEBITED);
        this.status = TransferStatus.COMPLETED;
    }

    /**
     * Called when any step in the transfer fails.
     * If the source was already debited, a reversal must follow.
     */
    public void markFailed(String reason) {
        if (status == TransferStatus.COMPLETED || status == TransferStatus.REVERSED) {
            throw new IllegalStateException(
                    "Cannot fail a transfer in status: " + status);
        }
        this.status = TransferStatus.FAILED;
        this.failureReason = reason;
    }

    /**
     * Called when the source account has been re-credited after a failed debit.
     * Terminal state — transfer ends here after reversal.
     */
    public void markReversed() {
        requireStatus(TransferStatus.FAILED);
        this.status = TransferStatus.REVERSED;
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    public boolean isDebited() {
        return status == TransferStatus.DEBITED;
    }

    public boolean isFailed() {
        return status == TransferStatus.FAILED;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public TransferId getId() {
        return id;
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

    public TransferStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public AuditMetadata getAudit() {
        return audit;
    }

    // -------------------------------------------------------------------------
    // Guards
    // -------------------------------------------------------------------------

    private void requireStatus(TransferStatus expected) {
        if (this.status != expected) {
            throw new IllegalStateException(
                    "Transfer operation requires status " + expected
                            + ", current: " + this.status);
        }
    }

    private void validateAccounts() {
        if (sourceAccountNumber.equals(destinationAccountNumber)) {
            throw new IllegalArgumentException(
                    "Source and destination accounts must be different");
        }
    }

    private void validateAmount() {
        if (!amount.isPositive()) {
            throw new IllegalArgumentException(
                    "Transfer amount must be positive, got: " + amount);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Transfer other))
            return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Transfer{id=" + id + ", from=" + sourceAccountNumber
                + ", to=" + destinationAccountNumber
                + ", amount=" + amount + ", status=" + status + "}";
    }
}