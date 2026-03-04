// src/main/java/com/nairacore/corebankingapi/account/domain/Account.java
package com.nairacore.corebankingapi.account.domain;

import java.math.BigDecimal;

public class Account {

    private final String accountNumber;
    private BigDecimal balance;
    private AccountStatus status;
    private final Integer version;

    public Account(String accountNumber, BigDecimal initialBalance, Integer version) {
        this.accountNumber = accountNumber;
        this.balance = initialBalance != null ? initialBalance : BigDecimal.ZERO;
        // this.balance = balance;
        this.version = version;
        this.status = AccountStatus.ACTIVE;
    }

    public Integer getVersion() {
        return version;
    }

    // === Factory method for NEW accounts ===
    public static Account open(String accountNumber, BigDecimal initialDeposit) {
        if (initialDeposit == null || initialDeposit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial deposit cannot be negative");
        }
        // Starts with version 0
        return new Account(accountNumber, initialDeposit, 0);
    }

    // Business behaviors live inside the domain model!
    public void deposit(BigDecimal amount) {
        if (this.status != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero");
        }
        this.balance = this.balance.add(amount);
    }

    public void withdraw(BigDecimal amount, BigDecimal minimumBalance) {
        if (this.status != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active");
        }

        BigDecimal balanceAfterWithdrawal = this.balance.subtract(amount);
        if (balanceAfterWithdrawal.compareTo(minimumBalance) < 0) {
            throw new IllegalStateException("Insufficient funds to maintain minimum balance");
        }
        this.balance = balanceAfterWithdrawal;
    }

    // Getters
    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public enum AccountStatus {
        ACTIVE, FROZEN, CLOSED
    }
}