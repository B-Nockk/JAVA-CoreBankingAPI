// account-module/src/main/java/com/coreledger/account/application/port/out/LoadAccountPort.java
package com.coreledger.account.application.port.out;

import java.util.List;
import java.util.Optional;

import com.coreledger.account.domain.model.Account;
import com.coreledger.account.domain.model.AccountId;

/**
 * Outbound port — defines what the application needs from persistence
 * in order to load an Account.
 *
 * The application service depends on this interface.
 * The JPA persistence adapter implements it.
 *
 * Optional is used deliberately:
 * - The port returns Optional because "not found" is a valid outcome
 * at the persistence level. The service decides what to do with it
 * (typically throw AccountNotFoundException).
 * - This keeps the exception out of the port contract — ports are neutral,
 * policy decisions belong in the service.
 */
public interface LoadAccountPort {

    Optional<Account> findById(AccountId id);

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findAll();
}