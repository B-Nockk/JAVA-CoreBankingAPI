// account-module/src/main/java/com/coreledger/account/domain/model/Transaction.java
package com.coreledger.account.domain.model;

import java.util.Objects;
import java.util.UUID;

import com.coreledger.shared.domain.AuditMetadata;
import com.coreledger.shared.domain.Money;

/**
 * Entity representing a single immutable ledger entry on an account.
 *
 * This is a child entity of the Account aggregate — it must never be
 * created, accessed, or modified from outside the Account class.
 * The Account aggregate root controls all access to its transactions.
 *
 * Immutability is structural:
 * - No setters
 * - All fields set at construction and final
 * - Once recorded, a transaction is permanent — it is never updated or deleted
 *
 * Balance is NOT stored here directly, but balanceAfter is recorded at the
 * moment of the transaction. This serves two purposes:
 * 1. Point-in-time auditability — you can see what the balance was after
 * any given transaction without replaying the entire ledger
 * 2. Integrity checks — if balanceAfter of row N doesn't match the sum
 * of all rows up to N, something is wrong
 */
public final class Transaction {

    private String transactionId;
    private final AccountId accountId;
    private final TransactionType type;
    private final Money amount;
    private final Money balanceAfter;
    private final String reference; // e.g. transfer ID, deposit reference
    private final AuditMetadata audit;

    private Transaction(
            AccountId accountId,
            TransactionType type,
            Money amount,
            Money balanceAfter,
            String reference,
            AuditMetadata audit) {
        this.transactionId = UUID.randomUUID().toString();
        this.accountId = Objects.requireNonNull(accountId);
        this.type = Objects.requireNonNull(type);
        this.amount = Objects.requireNonNull(amount);
        this.balanceAfter = Objects.requireNonNull(balanceAfter);
        this.reference = Objects.requireNonNull(reference);
        this.audit = Objects.requireNonNull(audit);
    }

    // -------------------------------------------------------------------------
    // Package-private factories — only Account (same package) can create these
    // -------------------------------------------------------------------------

    static Transaction deposit(AccountId accountId, Money amount, Money balanceAfter,
            String reference, String initiatedBy) {
        return new Transaction(accountId, TransactionType.DEPOSIT,
                amount, balanceAfter, reference, AuditMetadata.now(initiatedBy));
    }

    static Transaction withdrawal(AccountId accountId, Money amount, Money balanceAfter,
            String reference, String initiatedBy) {
        return new Transaction(accountId, TransactionType.WITHDRAWAL,
                amount, balanceAfter, reference, AuditMetadata.now(initiatedBy));
    }

    static Transaction transferIn(AccountId accountId, Money amount, Money balanceAfter,
            String transferId, String initiatedBy) {
        return new Transaction(accountId, TransactionType.TRANSFER_IN,
                amount, balanceAfter, transferId, AuditMetadata.now(initiatedBy));
    }

    static Transaction transferOut(AccountId accountId, Money amount, Money balanceAfter,
            String transferId, String initiatedBy) {
        return new Transaction(accountId, TransactionType.TRANSFER_OUT,
                amount, balanceAfter, transferId, AuditMetadata.now(initiatedBy));
    }

    public static Transaction reconstitute(
            String transactionId,
            AccountId accountId,
            TransactionType type,
            Money amount,
            Money balanceAfter,
            String reference,
            AuditMetadata audit) {
        Transaction tx = new Transaction(accountId, type, amount, balanceAfter, reference, audit);
        tx.transactionId = transactionId;
        return tx;
    }
    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public String getTransactionId() {
        return transactionId;
    }

    public AccountId getAccountId() {
        return accountId;
    }

    public TransactionType getType() {
        return type;
    }

    public Money getAmount() {
        return amount;
    }

    public Money getBalanceAfter() {
        return balanceAfter;
    }

    public String getReference() {
        return reference;
    }

    public AuditMetadata getAudit() {
        return audit;
    }

    public String getInitiatedBy() {
        return audit.getCreatedBy();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Transaction other))
            return false;
        return Objects.equals(transactionId, other.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString() {
        return "Transaction{id='" + transactionId + "', type=" + type
                + ", amount=" + amount + ", balanceAfter=" + balanceAfter + "}";
    }
}