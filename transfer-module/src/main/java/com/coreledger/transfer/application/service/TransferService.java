// transfer-module/src/main/java/com/coreledger/transfer/application/service/TransferService.java
package com.coreledger.transfer.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coreledger.shared.DomainEventPublisher;
import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.shared.domain.Money;
import com.coreledger.shared.events.MoneyDeposited;
import com.coreledger.shared.events.MoneyWithdrawn;
import com.coreledger.shared.events.TransferCompleted;
import com.coreledger.shared.events.TransferFailed;
import com.coreledger.shared.events.TransferReversed;
import com.coreledger.shared.kafka.EventDeserializer;
import com.coreledger.shared.kafka.EventEnvelope;
import com.coreledger.transfer.application.port.in.GetTransferUseCase;
import com.coreledger.transfer.application.port.in.InitiateTransferUseCase;
import com.coreledger.transfer.application.port.out.AccountVerificationPort;
import com.coreledger.transfer.application.port.out.LoadTransferPort;
import com.coreledger.transfer.application.port.out.SaveTransferPort;
import com.coreledger.transfer.domain.exceptions.InvalidTransferException;
import com.coreledger.transfer.domain.exceptions.TransferNotFoundException;
import com.coreledger.transfer.domain.model.Transfer;
import com.coreledger.transfer.domain.model.TransferId;
import com.coreledger.transfer.domain.model.TransferStatus;

/**
 * Application service for the transfer bounded context.
 *
 * Choreography flow:
 *
 * execute()
 * → saves Transfer(INITIATED)
 * → publishes TransferInitiated
 *
 * onAccountEvent receives MoneyWithdrawn
 * → marks Transfer(DEBITED)
 *
 * onAccountEvent receives MoneyDeposited
 * → marks Transfer(COMPLETED)
 * → publishes TransferCompleted
 *
 * onAccountEvent receives TransferReversed
 * → marks Transfer(REVERSED)
 */
@Service
public class TransferService implements InitiateTransferUseCase, GetTransferUseCase {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final LoadTransferPort loadTransferPort;
    private final SaveTransferPort saveTransferPort;
    private final AccountVerificationPort accountVerificationPort;
    private final DomainEventPublisher eventPublisher;
    private final EventDeserializer eventDeserializer;

    public TransferService(
            LoadTransferPort loadTransferPort,
            SaveTransferPort saveTransferPort,
            AccountVerificationPort accountVerificationPort,
            DomainEventPublisher eventPublisher,
            EventDeserializer eventDeserializer) {
        this.loadTransferPort = loadTransferPort;
        this.saveTransferPort = saveTransferPort;
        this.accountVerificationPort = accountVerificationPort;
        this.eventPublisher = eventPublisher;
        this.eventDeserializer = eventDeserializer;
    }

    // ── Kafka listener ────────────────────────────────────────────────────────

    @KafkaListener(topics = "${kafka.topics.account-events}", groupId = "coreledger-transfer", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void onAccountEvent(@Payload EventEnvelope envelope) {
        DomainEvent event = eventDeserializer.deserialize(envelope);
        if (event == null)
            return;

        if (event instanceof MoneyWithdrawn e) {
            handleMoneyWithdrawn(e);
        } else if (event instanceof MoneyDeposited e) {
            handleMoneyDeposited(e);
        } else if (event instanceof TransferReversed e) {
            handleTransferReversed(e);
        }
        // AccountCreated etc. intentionally ignored
    }

    private void handleMoneyWithdrawn(MoneyWithdrawn event) {
        String reference = event.getReference();
        if (!isTransferId(reference)) {
            log.debug("Ignoring MoneyWithdrawn with non-transfer reference='{}'", reference);
            return;
        }

        String transferId = event.getReference();
        log.debug("Handling MoneyWithdrawn for transferId={}", transferId);

        loadTransferPort.findById(TransferId.of(transferId)).ifPresent(transfer -> {
            if (transfer.getStatus() != TransferStatus.INITIATED)
                return;
            transfer.markDebited();
            saveTransferPort.save(transfer);
            log.info("Transfer {} marked DEBITED", transferId);
        });
    }

    private void handleMoneyDeposited(MoneyDeposited event) {
        String reference = event.getReference();
        if (!isTransferId(reference)) {
            log.debug("Ignoring MoneyDeposited with non-transfer reference='{}'", reference);
            return;
        }

        String transferId = event.getReference();
        log.debug("Handling MoneyDeposited for transferId={}", transferId);

        loadTransferPort.findById(TransferId.of(transferId)).ifPresent(transfer -> {
            // Accept INITIATED or DEBITED — both events may arrive near-simultaneously
            // since TransferEventHandler does debit+credit in one transaction
            if (transfer.getStatus() != TransferStatus.DEBITED
                    && transfer.getStatus() != TransferStatus.INITIATED) {
                log.debug("Ignoring MoneyDeposited for transfer {} in status {}",
                        transferId, transfer.getStatus());
                return;
            }
            try {
                transfer.markCompleted();
                saveTransferPort.save(transfer);
                eventPublisher.publishTransferEvent(new TransferCompleted(
                        transfer.getId().toString(),
                        transfer.getSourceAccountNumber(),
                        transfer.getDestinationAccountNumber(),
                        transfer.getAmount()));
                log.info("Transfer {} COMPLETED", transferId);
            } catch (Exception e) {
                log.error("Failed to complete transfer {}: {}", transferId, e.getMessage(), e);
                handleTransferFailure(transfer, "Failed to mark completed: " + e.getMessage());
            }
        });
    }

    private void handleTransferReversed(TransferReversed event) {
        String transferId = event.getAggregateId();
        log.debug("Handling TransferReversed for transferId={}", transferId);

        loadTransferPort.findById(TransferId.of(transferId)).ifPresent(transfer -> {
            transfer.markReversed();
            saveTransferPort.save(transfer);
            log.info("Transfer {} REVERSED", transferId);
        });
    }

    // ── InitiateTransferUseCase ───────────────────────────────────────────────

    @Override
    @Transactional
    public InitiateTransferUseCase.TransferResult execute(Command command) {
        AccountVerificationPort.AccountView source = accountVerificationPort
                .findActiveAccount(command.sourceAccountNumber());
        AccountVerificationPort.AccountView destination = accountVerificationPort
                .findActiveAccount(command.destinationAccountNumber());

        if (source.currency() != destination.currency()) {
            throw new InvalidTransferException(
                    "Cross-currency transfers not supported: %s → %s"
                            .formatted(source.currency(), destination.currency()));
        }

        Money amount = Money.of(command.amount(), source.currency());
        Transfer transfer = Transfer.initiate(
                command.sourceAccountNumber(),
                command.destinationAccountNumber(),
                amount,
                command.initiatedBy());

        Transfer saved = saveTransferPort.save(transfer);

        eventPublisher.publishTransferEvent(new com.coreledger.shared.events.TransferInitiated(
                saved.getId().toString(),
                saved.getSourceAccountNumber(),
                saved.getDestinationAccountNumber(),
                saved.getAmount()));

        log.info("Transfer {} initiated from {} to {} amount={}",
                saved.getId(), saved.getSourceAccountNumber(),
                saved.getDestinationAccountNumber(), saved.getAmount());

        return toInitiateResult(saved);
    }

    // ── GetTransferUseCase ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public GetTransferUseCase.TransferResult getById(String transferId) {
        return loadTransferPort.findById(TransferId.of(transferId))
                .map(this::toGetResult)
                .orElseThrow(() -> new TransferNotFoundException(transferId));
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private void handleTransferFailure(Transfer transfer, String reason) {
        boolean wasDebited = transfer.isDebited();
        transfer.markFailed(reason);
        saveTransferPort.save(transfer);

        if (wasDebited) {
            eventPublisher.publishTransferEvent(new TransferFailed(
                    transfer.getId().toString(),
                    transfer.getSourceAccountNumber(),
                    transfer.getAmount(),
                    reason));
        }

        log.warn("Transfer {} FAILED: {}", transfer.getId(), reason);
    }

    private InitiateTransferUseCase.TransferResult toInitiateResult(Transfer transfer) {
        return new InitiateTransferUseCase.TransferResult(
                transfer.getId().toString(),
                transfer.getSourceAccountNumber(),
                transfer.getDestinationAccountNumber(),
                transfer.getAmount().getAmount(),
                transfer.getAmount().getCurrency(),
                transfer.getStatus().name(),
                transfer.getAudit().getCreatedAt());
    }

    private GetTransferUseCase.TransferResult toGetResult(Transfer transfer) {
        return new GetTransferUseCase.TransferResult(
                transfer.getId().toString(),
                transfer.getSourceAccountNumber(),
                transfer.getDestinationAccountNumber(),
                transfer.getAmount().getAmount(),
                transfer.getAmount().getCurrency(),
                transfer.getStatus().name(),
                transfer.getFailureReason(),
                transfer.getAudit().getCreatedAt());
    }

    private static boolean isTransferId(String reference) {
        if (reference == null)
            return false;
        try {
            java.util.UUID.fromString(reference);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}