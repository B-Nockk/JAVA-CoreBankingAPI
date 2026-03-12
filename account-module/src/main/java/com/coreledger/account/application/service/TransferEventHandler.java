// account-module/src/main/java/com/coreledger/account/application/service/TransferEventHandler.java
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
import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.shared.events.MoneyDeposited;
import com.coreledger.shared.events.MoneyWithdrawn;
import com.coreledger.shared.events.TransferFailed;
import com.coreledger.shared.events.TransferInitiated;
import com.coreledger.shared.events.TransferReversed;
import com.coreledger.shared.kafka.EventDeserializer;
import com.coreledger.shared.kafka.EventEnvelope;

/**
 * Consumes transfer-domain events and executes account-side operations.
 *
 * Listens on: coreledger.transfer.events
 * Publishes to: coreledger.account.events
 *
 * The @KafkaListener receives an EventEnvelope (raw wrapper).
 * eventDeserializer.deserialize() resolves the concrete event type.
 * We then dispatch on instanceof — fully type-safe, no reflection in handlers.
 */
@Service
public class TransferEventHandler {

    private static final Logger log = LoggerFactory.getLogger(TransferEventHandler.class);

    private final LoadAccountPort loadAccountPort;
    private final SaveAccountPort saveAccountPort;
    private final DomainEventPublisher eventPublisher;
    private final EventDeserializer eventDeserializer;

    public TransferEventHandler(
            LoadAccountPort loadAccountPort,
            SaveAccountPort saveAccountPort,
            DomainEventPublisher eventPublisher,
            EventDeserializer eventDeserializer) {
        this.loadAccountPort = loadAccountPort;
        this.saveAccountPort = saveAccountPort;
        this.eventPublisher = eventPublisher;
        this.eventDeserializer = eventDeserializer;

    }

    @KafkaListener(topics = "${kafka.topics.transfer-events}", groupId = "coreledger-account", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void onTransferEvent(@Payload EventEnvelope envelope) {
        DomainEvent event = eventDeserializer.deserialize(envelope);
        if (event == null)
            return; // unknown type or deserialization error — already logged

        if (event instanceof TransferInitiated e) {
            handleTransferInitiated(e);
        } else if (event instanceof TransferFailed e) {
            handleTransferFailed(e);
        }
        // Other event types on this topic are intentionally ignored
    }

    private void handleTransferInitiated(TransferInitiated event) {
        String transferId = event.getAggregateId();
        log.info("Handling TransferInitiated transferId={}", transferId);

        // Debit source
        Account source = loadAccountPort
                .findByAccountNumber(event.getSourceAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(event.getSourceAccountNumber()));

        Transaction debitTx = source.debitTransfer(event.getAmount(), transferId, "TRANSFER_SYSTEM");
        saveAccountPort.save(source);

        eventPublisher.publishAccountEvent(new MoneyWithdrawn(
                source.getId().toString(),
                source.getAccountNumber(),
                debitTx.getAmount(),
                debitTx.getBalanceAfter(),
                transferId));

        // Credit destination
        Account destination = loadAccountPort
                .findByAccountNumber(event.getDestinationAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(event.getDestinationAccountNumber()));

        Transaction creditTx = destination.creditTransfer(event.getAmount(), transferId, "TRANSFER_SYSTEM");
        saveAccountPort.save(destination);

        eventPublisher.publishAccountEvent(new MoneyDeposited(
                destination.getId().toString(),
                destination.getAccountNumber(),
                creditTx.getAmount(),
                creditTx.getBalanceAfter(),
                transferId));

        log.info("TransferInitiated handled — debit and credit applied transferId={}", transferId);
    }

    private void handleTransferFailed(TransferFailed event) {
        String transferId = event.getAggregateId();
        log.warn("Handling TransferFailed reversal transferId={}", transferId);

        Account source = loadAccountPort
                .findByAccountNumber(event.getSourceAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(event.getSourceAccountNumber()));

        source.creditTransfer(event.getAmount(), transferId + "-REVERSAL", "TRANSFER_SYSTEM");
        saveAccountPort.save(source);

        eventPublisher.publishAccountEvent(new TransferReversed(
                transferId,
                source.getAccountNumber(),
                event.getAmount()));

        log.info("Transfer {} reversed — source re-credited", transferId);
    }
}