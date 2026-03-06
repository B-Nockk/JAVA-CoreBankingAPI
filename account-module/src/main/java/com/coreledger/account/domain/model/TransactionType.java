package com.coreledger.account.domain.model;

/**
 * The nature of a ledger entry on an account.
 *
 * DEPOSIT — money coming into the account from an external source
 * WITHDRAWAL — money leaving the account to an external destination
 * TRANSFER_IN — credit leg of an internal transfer (receiving side)
 * TRANSFER_OUT — debit leg of an internal transfer (sending side)
 *
 * Transfers are split into two transaction types because each account
 * in a transfer gets its own ledger entry. The transfer-module owns
 * the orchestration; the account-module records each leg independently.
 * This keeps the account ledger self-contained and auditable on its own.
 */
public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER_IN,
    TRANSFER_OUT
}