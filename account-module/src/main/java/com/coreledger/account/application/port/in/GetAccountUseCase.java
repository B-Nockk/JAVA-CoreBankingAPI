// account-module/src/main/java/com/coreledger/account/application/port/in/GetAccountUseCase.java
package com.coreledger.account.application.port.in;

import java.time.Instant;
import java.util.List;

import com.coreledger.shared.domain.Currency;
import com.coreledger.shared.domain.Money;

/**
 * Inbound port — defines the contract for retrieving account information.
 *
 * Two query methods:
 * - getByAccountNumber: customer-facing lookup (what they see on their app)
 * - getAllAccounts: admin/internal listing
 *
 * Queries return a dedicated Result record, not the Account domain object.
 * The domain object must never leak out of the application layer — it carries
 * behaviour and invariants that make no sense outside the domain. The Result
 * is a plain data snapshot: safe to serialise, safe to expose.
 */
public interface GetAccountUseCase {

    AccountResult getByAccountNumber(String accountNumber);

    List<AccountResult> getAllAccounts();

    record AccountResult(
            String accountId,
            String accountNumber,
            String ownerName,
            Currency currency,
            Money balance,
            String status,
            Instant createdAt) {
    }
}