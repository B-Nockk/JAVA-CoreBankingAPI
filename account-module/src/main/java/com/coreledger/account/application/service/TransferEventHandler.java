// account-module/src/main/java/com/coreledger/account/application/service/TransferEventHandler-kafka.java
package com.coreledger.account.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coreledger.account.application.port.out.LoadAccountPort;
import com.coreledger.account.application.port.out.SaveAccountPort;
import com.coreledger.account.domain.exceptions.AccountNotFoundException;
import com.coreledger.account.domain.model.Account;
import com.coreledger.account.domain.model.Transaction;
import com.coreledger.shared.DomainEventPublisher;
import com.coreledger.shared.events.MoneyDeposited;
import com.coreledger.shared.events.MoneyWithdrawn;
import com.coreledger.shared.events.TransferFailed;
import com.coreledger.shared.events.TransferInitiated;
import com.coreledger.shared.events.TransferReversed;

/**
 * Consumes transfer-domain events from Kafka and executes account-side
 * operations.
 *
 * Listens on: coreledger.transfer.events
 * Publishes to: coreledger.account.events (via EventPublisher)
 *
 * Each @KafkaListener method is its own transaction — if it fails, only that
 * message's processing rolls back. Kafka will redeliver it (at-least-once).
 *
 * groupId = "coreledger-account" — separate from transfer-module's consumer
 * group
 * so both modules can independently consume from the same topic if needed.
 */
@Service
public class TransferEventHandler {

    private static final Logger log = LoggerFactory.getLogger(TransferEventHandler.class);

    private final LoadAccountPort loadAccountPort;
    private final SaveAccountPort saveAccountPort;
    private final DomainEventPublisher eventPublisher;

    public TransferEventHandler(LoadAccountPort loadAccountPort,
            SaveAccountPort saveAccountPort,
            DomainEventPublisher eventPublisher) {
        this.loadAccountPort = loadAccountPort;
        this.saveAccountPort = saveAccountPort;
        this.eventPublisher = eventPublisher;
    }

    @KafkaListener(topics = "${kafka.topics.transfer-events}", groupId = "coreledger-account", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void onTransferInitiated(@Payload TransferInitiated event) {
        String transferId = event.getAggregateId();
        log.info("Handling TransferInitiated for transfer {}", transferId);

        // Step 1: Debit source account
        Account source = loadAccountPort.findByAccountNumber(event.getSourceAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(event.getSourceAccountNumber()));

        Transaction debitTx = source.debitTransfer(
                event.getAmount(), transferId, "TRANSFER_SYSTEM");
        saveAccountPort.save(source);

        eventPublisher.publishAccountEvent(new MoneyWithdrawn(
                source.getId().toString(),
                source.getAccountNumber(),
                debitTx.getAmount(),
                debitTx.getBalanceAfter(),
                transferId));

        // Step 2: Credit destination account
        Account destination = loadAccountPort
                .findByAccountNumber(event.getDestinationAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(
                        event.getDestinationAccountNumber()));

        Transaction creditTx = destination.creditTransfer(
                event.getAmount(), transferId, "TRANSFER_SYSTEM");
        saveAccountPort.save(destination);

        eventPublisher.publishAccountEvent(new MoneyDeposited(
                destination.getId().toString(),
                destination.getAccountNumber(),
                creditTx.getAmount(),
                creditTx.getBalanceAfter(),
                transferId));

        log.info("Transfer {} — debit and credit applied", transferId);
    }

    @KafkaListener(topics = "${kafka.topics.transfer-events}", groupId = "coreledger-account", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void onTransferFailed(@Payload TransferFailed event) {
        String transferId = event.getAggregateId();
        log.warn("Handling TransferFailed reversal for transfer {}", transferId);

        Account source = loadAccountPort
                .findByAccountNumber(event.getSourceAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(
                        event.getSourceAccountNumber()));

        // Re-credit source — money goes back
        source.creditTransfer(event.getAmount(), transferId + "-REVERSAL", "TRANSFER_SYSTEM");
        saveAccountPort.save(source);

        eventPublisher.publishAccountEvent(new TransferReversed(
                transferId,
                source.getAccountNumber(),
                event.getAmount()));

        log.info("Transfer {} reversed — source account re-credited", transferId);
    }
}