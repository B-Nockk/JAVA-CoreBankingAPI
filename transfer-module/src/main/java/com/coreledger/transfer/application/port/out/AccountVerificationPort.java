// transfer-module/src/main/java/com/coreledger/transfer/application/port/out/AccountVerificationPort.java
package com.coreledger.transfer.application.port.out;

import com.coreledger.shared.domain.Currency;

/**
 * Outbound port — defines what the transfer module needs to know
 * about accounts before initiating a transfer.
 *
 * This is the clean cross-module boundary.
 * Transfer-module defines what it needs (this interface).
 * The infrastructure adapter in app/ implements it by calling AccountService.
 *
 * Why not just call AccountService directly?
 * If transfer-module imported AccountService, it would depend on
 * account-module.
 * That dependency would make it impossible to extract transfer-module into
 * a separate microservice without dragging account-module with it.
 *
 * With this port, the dependency direction is:
 * transfer-module defines the port (what it needs)
 * app/ provides the adapter (how it's satisfied in this deployment)
 *
 * In a microservice deployment, the adapter becomes an HTTP client
 * calling the account service over the network. Transfer-module doesn't change.
 *
 * AccountView is a minimal projection — only what transfer needs,
 * nothing more. Transfer has no business knowing account owner names
 * or full transaction history.
 */
public interface AccountVerificationPort {

    AccountView findActiveAccount(String accountNumber);

    record AccountView(
            String accountNumber,
            Currency currency,
            boolean isActive) {
    }
}