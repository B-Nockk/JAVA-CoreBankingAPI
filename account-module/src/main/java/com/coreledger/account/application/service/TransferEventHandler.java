// account-module/src/main/java/com/coreledger/account/application/service/TransferEventHandler.java
package com.coreledger.account.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coreledger.account.application.port.out.LoadAccountPort;
import com.coreledger.account.application.port.out.SaveAccountPort;
import com.coreledger.account.domain.exceptions.AccountNotFoundException;
import com.coreledger.account.domain.model.Account;
import com.coreledger.account.domain.model.Transaction;
import com.coreledger.shared.events.MoneyDeposited;
import com.coreledger.shared.events.MoneyWithdrawn;
import com.coreledger.shared.events.TransferFailed;
import com.coreledger.shared.events.TransferInitiated;

/**
 * Event handler in account-module that reacts to transfer lifecycle events.
 *
 * This is the account-module's side of the choreography.
 * It listens for events published by transfer-module and responds by
 * performing account operations, then publishing its own events back.
 *
 * Flow this handler participates in:
 *
 * onTransferInitiated()
 * → debits source account
 * → publishes MoneyWithdrawn (transfer-module reacts, marks DEBITED,
 * and account-module's own handler then credits destination)
 *
 * onTransferInitiated() continued — after debit succeeds
 * → credits destination account
 * → publishes MoneyDeposited (transfer-module reacts, marks COMPLETED)
 *
 * onTransferFailed()
 * → re-credits source account (reversal)
 * → publishes TransferReversed (transfer-module reacts, marks REVERSED)
 *
 * Why debit and credit in the same handler?
 * In the in-process synchronous model, publishing MoneyWithdrawn and
 * immediately having TransferService.onMoneyWithdrawn() react would
 * work but creates unnecessary event round-trips for the credit step.
 * Since we're synchronous, we complete both legs here and publish the
 * final MoneyDeposited for transfer-module to close out the transfer.
 * When we move to Kafka, this handler splits into two consumers naturally.
 */
@Service
@Transactional
public class TransferEventHandler {

    private static final Logger log = LoggerFactory.getLogger(TransferEventHandler.class);

    private final LoadAccountPort loadAccountPort;
    private final SaveAccountPort saveAccountPort;
    private final ApplicationEventPublisher eventPublisher;

    public TransferEventHandler(
            LoadAccountPort loadAccountPort,
            SaveAccountPort saveAccountPort,
            ApplicationEventPublisher eventPublisher) {
        this.loadAccountPort = loadAccountPort;
        this.saveAccountPort = saveAccountPort;
        this.eventPublisher = eventPublisher;
    }

    @EventListener
    public void onTransferInitiated(TransferInitiated event) {
        String transferId = event.getAggregateId();
        log.info("Handling TransferInitiated for transfer {}", transferId);

        // Step 1: Debit source
        Account source = loadAccountPort.findByAccountNumber(event.getSourceAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(event.getSourceAccountNumber()));

        Transaction debitTx = source.debitTransfer(
                event.getAmount(),
                transferId,
                "TRANSFER_SYSTEM");
        saveAccountPort.save(source);

        eventPublisher.publishEvent(new MoneyWithdrawn(
                source.getId().toString(),
                source.getAccountNumber(),
                debitTx.getAmount(),
                debitTx.getBalanceAfter(),
                transferId // reference = transferId so transfer-module can correlate
        ));

        // Step 2: Credit destination
        Account destination = loadAccountPort
                .findByAccountNumber(event.getDestinationAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(
                        event.getDestinationAccountNumber()));

        Transaction creditTx = destination.creditTransfer(
                event.getAmount(),
                transferId,
                "TRANSFER_SYSTEM");
        saveAccountPort.save(destination);

        eventPublisher.publishEvent(new MoneyDeposited(
                destination.getId().toString(),
                destination.getAccountNumber(),
                creditTx.getAmount(),
                creditTx.getBalanceAfter(),
                transferId // reference = transferId for correlation
        ));

        log.info("Transfer {} — debit and credit completed", transferId);
    }

    @EventListener
    public void onTransferFailed(TransferFailed event) {
        String transferId = event.getAggregateId();
        log.warn("Handling TransferFailed reversal for transfer {}", transferId);

        Account source = loadAccountPort
                .findByAccountNumber(event.getSourceAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(
                        event.getSourceAccountNumber()));

        // Re-credit source — money goes back
        source.creditTransfer(event.getAmount(), transferId + "-REVERSAL", "TRANSFER_SYSTEM");
        saveAccountPort.save(source);

        eventPublisher.publishEvent(new com.coreledger.shared.events.TransferReversed(
                transferId,
                source.getAccountNumber(),
                event.getAmount()));

        log.info("Transfer {} reversed — source account re-credited", transferId);
    }
}