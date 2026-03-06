// account-module/src/main/java/com/coreledger/account/domain/model/Account.java
package com.coreledger.account.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.coreledger.shared.domain.AuditMetadata;
import com.coreledger.shared.domain.Currency;
import com.coreledger.shared.domain.Money;

/**
 * Aggregate Root for the Account bounded context.
 *
 * This is the ONLY way into account state. Nothing outside this class
 * creates or touches Transaction objects directly. The aggregate root
 * owns and protects all invariants (business rules that must always hold).
 *
 * Invariants enforced here:
 * - An account cannot go below zero (no overdraft in v1)
 * - A FROZEN or CLOSED account cannot be debited
 * - A CLOSED account cannot be credited either
 * - Currency must be consistent — you cannot deposit USD into an NGN account
 *
 * Balance is a derived property:
 * - Not stored as a field that gets mutated
 * - Computed from the last transaction's balanceAfter
 * - If there are no transactions, balance is zero
 * This is the append-only ledger pattern discussed at the start.
 *
 * accountNumber is separate from accountId:
 * - accountId (UUID): internal system identifier, used in inter-service calls
 * - accountNumber (String): human-facing identifier shown to customers
 * (e.g. "0123456789" in Nigerian bank format)
 */
public class Account {

    private final AccountId id;
    private final String accountNumber;
    private final String ownerName;
    private final Currency currency;
    private AccountStatus status;
    private final List<Transaction> transactions;
    private final AuditMetadata audit;

    // -------------------------------------------------------------------------
    // Construction — package-private, use factory method
    // -------------------------------------------------------------------------

    private Account(
            AccountId id,
            String accountNumber,
            String ownerName,
            Currency currency,
            AuditMetadata audit) {
        this.id = Objects.requireNonNull(id);
        this.accountNumber = Objects.requireNonNull(accountNumber);
        this.ownerName = Objects.requireNonNull(ownerName);
        this.currency = Objects.requireNonNull(currency);
        this.status = AccountStatus.ACTIVE;
        this.transactions = new ArrayList<>();
        this.audit = Objects.requireNonNull(audit);
    }

    /**
     * Factory method for opening a new account.
     * This is the only legitimate way to create an Account in the domain.
     */
    public static Account open(
            String accountNumber,
            String ownerName,
            Currency currency,
            String openedBy) {
        return new Account(
                AccountId.generate(),
                accountNumber,
                ownerName,
                currency,
                AuditMetadata.now(openedBy));
    }

    /**
     * Reconstruction factory — used by the persistence adapter to rebuild
     * an Account from stored data. Not for opening new accounts.
     */
    public static Account reconstitute(
            AccountId id,
            String accountNumber,
            String ownerName,
            Currency currency,
            AccountStatus status,
            List<Transaction> transactions,
            AuditMetadata audit) {
        Account account = new Account(id, accountNumber, ownerName, currency, audit);
        account.status = status;
        account.transactions.addAll(transactions);
        return account;
    }

    // -------------------------------------------------------------------------
    // Domain behaviour
    // -------------------------------------------------------------------------

    public Transaction deposit(Money amount, String reference, String initiatedBy) {
        requireStatus(AccountStatus.ACTIVE, AccountStatus.FROZEN); // credits allowed on FROZEN
        requireNotClosed();
        requireSameCurrency(amount);
        requirePositiveAmount(amount);

        Money newBalance = getBalance().add(amount);
        Transaction tx = Transaction.deposit(id, amount, newBalance, reference, initiatedBy);
        transactions.add(tx);
        return tx;
    }

    public Transaction withdraw(Money amount, String reference, String initiatedBy) {
        requireActive();
        requireSameCurrency(amount);
        requirePositiveAmount(amount);
        requireSufficientFunds(amount);

        Money newBalance = getBalance().subtract(amount);
        Transaction tx = Transaction.withdrawal(id, amount, newBalance, reference, initiatedBy);
        transactions.add(tx);
        return tx;
    }

    public Transaction creditTransfer(Money amount, String transferId, String initiatedBy) {
        requireNotClosed();
        requireSameCurrency(amount);
        requirePositiveAmount(amount);

        Money newBalance = getBalance().add(amount);
        Transaction tx = Transaction.transferIn(id, amount, newBalance, transferId, initiatedBy);
        transactions.add(tx);
        return tx;
    }

    public Transaction debitTransfer(Money amount, String transferId, String initiatedBy) {
        requireActive();
        requireSameCurrency(amount);
        requirePositiveAmount(amount);
        requireSufficientFunds(amount);

        Money newBalance = getBalance().subtract(amount);
        Transaction tx = Transaction.transferOut(id, amount, newBalance, transferId, initiatedBy);
        transactions.add(tx);
        return tx;
    }

    public void freeze(String initiatedBy) {
        if (status == AccountStatus.CLOSED) {
            throw new IllegalStateException("Cannot freeze a closed account");
        }
        this.status = AccountStatus.FROZEN;
    }

    public void unfreeze(String initiatedBy) {
        if (status != AccountStatus.FROZEN) {
            throw new IllegalStateException("Account is not frozen");
        }
        this.status = AccountStatus.ACTIVE;
    }

    public void close(String initiatedBy) {
        if (status == AccountStatus.CLOSED) {
            throw new IllegalStateException("Account is already closed");
        }
        if (getBalance().isPositive()) {
            throw new IllegalStateException(
                    "Cannot close account with a positive balance — withdraw funds first");
        }
        this.status = AccountStatus.CLOSED;
    }

    // -------------------------------------------------------------------------
    // Derived state
    // -------------------------------------------------------------------------

    /**
     * Current balance derived from the transaction ledger.
     * Never stored — always computed. This is the core of the append-only design.
     */
    public Money getBalance() {
        if (transactions.isEmpty()) {
            return Money.zero(currency);
        }
        return transactions.get(transactions.size() - 1).getBalanceAfter();
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public AccountId getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public Currency getCurrency() {
        return currency;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public AuditMetadata getAudit() {
        return audit;
    }

    /** Returns an unmodifiable view — callers cannot mutate the transaction list */
    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    // -------------------------------------------------------------------------
    // Invariant guards — private, called before every operation
    // -------------------------------------------------------------------------

    private void requireActive() {
        if (status != AccountStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Operation requires ACTIVE account, current status: " + status);
        }
    }

    private void requireNotClosed() {
        if (status == AccountStatus.CLOSED) {
            throw new IllegalStateException("Operation not permitted on a CLOSED account");
        }
    }

    private void requireStatus(AccountStatus... allowed) {
        for (AccountStatus s : allowed) {
            if (this.status == s)
                return;
        }
        throw new IllegalStateException("Account status " + status + " does not permit this operation");
    }

    private void requireSameCurrency(Money amount) {
        if (amount.getCurrency() != this.currency) {
            throw new IllegalArgumentException(
                    "Currency mismatch: account is " + currency + ", amount is " + amount.getCurrency());
        }
    }

    private void requirePositiveAmount(Money amount) {
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Amount must be positive, got: " + amount);
        }
    }

    private void requireSufficientFunds(Money amount) {
        if (getBalance().isLessThan(amount)) {
            throw new IllegalStateException(
                    "Insufficient funds: balance is " + getBalance() + ", requested " + amount);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Account other))
            return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Account{id=" + id + ", accountNumber='" + accountNumber
                + "', status=" + status + ", balance=" + getBalance() + "}";
    }
}