// src/main/java/com/nairacore/corebankingapi/account/domain/Account.java
package com.nairacore.corebankingapi.account.domain;

import java.math.BigDecimal;

public class Account {

    private final String accountNumber;
    private BigDecimal balance;
    private AccountStatus status;

    public Account(String accountNumber, BigDecimal initialBalance) {
        this.accountNumber = accountNumber;
        this.balance = initialBalance != null ? initialBalance : BigDecimal.ZERO;
        this.status = AccountStatus.ACTIVE;
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

    // public void withdraw(BigDecimal amount) {
    // if (this.status != AccountStatus.ACTIVE) {
    // throw new IllegalStateException("Account is not active");
    // }

    // // Define the minimum balance rule
    // // TODO:: pull minimum balance from env
    // BigDecimal minimumBalance = new BigDecimal("1000.00");
    // BigDecimal balanceAfterWithdrawal = this.balance.subtract(amount);

    // if (balanceAfterWithdrawal.compareTo(minimumBalance) < 0) {
    // throw new IllegalStateException("Insufficient funds: Minimum balance of 1000
    // NGN must be maintained.");
    // }

    // this.balance = balanceAfterWithdrawal;
    // }

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