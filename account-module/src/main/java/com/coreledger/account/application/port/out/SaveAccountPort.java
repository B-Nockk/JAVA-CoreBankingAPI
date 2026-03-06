package com.coreledger.account.application.port.out;

import com.coreledger.account.domain.model.Account;

/**
 * Outbound port — defines what the application needs from persistence
 * in order to save Account state.
 *
 * A single save() method handles both creation and updates to account
 * metadata (status changes like freeze/close). The persistence adapter
 * figures out whether to INSERT or UPDATE — that's an infrastructure concern.
 *
 * Transactions (ledger entries) are saved as part of the Account aggregate —
 * saving the account also persists any new transactions added to it.
 * This preserves the aggregate boundary: you never save a Transaction
 * independently of its Account.
 */
public interface SaveAccountPort {

    Account save(Account account);
}