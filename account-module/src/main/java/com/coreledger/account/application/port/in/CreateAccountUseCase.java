// account-module/src/main/java/com/coreledger/account/application/port/in/CreateAccountUseCase.java
package com.coreledger.account.application.port.in;

import com.coreledger.shared.domain.Currency;

/**
 * Inbound port — defines the contract for opening a new account.
 *
 * This interface is what the web adapter (controller) calls.
 * The application service implements it.
 *
 * Why an interface and not just call the service directly?
 * The controller depends on this abstraction, not the concrete service.
 * This means you can swap, decorate, or mock the implementation without
 * touching the controller. It also makes the use case boundary explicit —
 * reading the port tells you exactly what the system can do, independent
 * of how it does it.
 *
 * The nested Command record is the input model for this use case.
 * Using a dedicated Command object (not raw parameters) means:
 * - The signature doesn't change if you add fields — you just add to the record
 * - Validation can be attached to the Command itself
 * - It reads as a deliberate business instruction, not a method call
 */
public interface CreateAccountUseCase {

    AccountCreatedResult execute(Command command);

    record Command(
            String ownerName,
            Currency currency,
            String openedBy // system/user initiating the action
    ) {
    }

    record AccountCreatedResult(
            String accountId,
            String accountNumber,
            String ownerName,
            Currency currency,
            String status) {
    }
}