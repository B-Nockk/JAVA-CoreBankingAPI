//
package com.coreledger.transfer.application.service;

import com.coreledger.shared.domain.Money;
import com.coreledger.shared.events.TransferCompleted;
import com.coreledger.shared.events.TransferFailed;
import com.coreledger.shared.events.TransferInitiated;
import com.coreledger.shared.events.TransferReversed;
import com.coreledger.shared.events.MoneyWithdrawn;
import com.coreledger.shared.events.MoneyDeposited;
import com.coreledger.transfer.application.port.in.GetTransferUseCase;
import com.coreledger.transfer.application.port.in.InitiateTransferUseCase;
import com.coreledger.transfer.application.port.out.AccountVerificationPort;
import com.coreledger.transfer.application.port.out.LoadTransferPort;
import com.coreledger.transfer.application.port.out.SaveTransferPort;
import com.coreledger.transfer.domain.exceptions.InvalidTransferException;
import com.coreledger.transfer.domain.exceptions.TransferNotFoundException;
import com.coreledger.transfer.domain.model.Transfer;
import com.coreledger.transfer.domain.model.TransferId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for the transfer bounded context.
 *
 * Two responsibilities:
 * 1. Initiating transfers (inbound use case)
 * 2. Reacting to account-module events to advance the transfer lifecycle
 *
 * The choreography flow this service participates in:
 *
 * initiate()
 * → saves Transfer(INITIATED)
 * → publishes TransferInitiated
 *
 * onMoneyWithdrawn() [listens to account-module event]
 * → marks Transfer(DEBITED)
 * → publishes credit instruction via TransferInitiated with debit confirmed
 * (account-module's handler will credit destination on MoneyWithdrawn)
 *
 * onMoneyDeposited() [listens to account-module event]
 * → marks Transfer(COMPLETED)
 * → publishes TransferCompleted
 *
 * onTransferFailed() [self-published or from account-module failure]
 * → marks Transfer(FAILED)
 * → publishes TransferFailed if source was already debited (triggers reversal)
 *
 * onTransferReversed() [listens to account-module event]
 * → marks Transfer(REVERSED)
 */
@Service
@Transactional
public class TransferService implements InitiateTransferUseCase, GetTransferUseCase {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final LoadTransferPort loadTransferPort;
    private final SaveTransferPort saveTransferPort;
    private final AccountVerificationPort accountVerificationPort;
    private final ApplicationEventPublisher eventPublisher;

    public TransferService(
            LoadTransferPort loadTransferPort,
            SaveTransferPort saveTransferPort,
            AccountVerificationPort accountVerificationPort,
            ApplicationEventPublisher eventPublisher) {
        this.loadTransferPort = loadTransferPort;
        this.saveTransferPort = saveTransferPort;
        this.accountVerificationPort = accountVerificationPort;
        this.eventPublisher = eventPublisher;
    }

    // -------------------------------------------------------------------------
    // InitiateTransferUseCase
    // -------------------------------------------------------------------------

    @Override
    public TransferResult execute(Command command) {
        // Verify both accounts exist and are active
        AccountVerificationPort.AccountView source = accountVerificationPort
                .findActiveAccount(command.sourceAccountNumber());
        AccountVerificationPort.AccountView destination = accountVerificationPort
                .findActiveAccount(command.destinationAccountNumber());

        // Enforce same-currency transfers in v1
        if (source.currency() != destination.currency()) {
            throw new InvalidTransferException(
                    "Cross-currency transfers not supported in v1: "
                            + source.currency() + " → " + destination.currency());
        }

        Money amount = Money.of(command.amount(), source.currency());

        Transfer transfer = Transfer.initiate(
                command.sourceAccountNumber(),
                command.destinationAccountNumber(),
                amount,
                command.initiatedBy());

        Transfer saved = saveTransferPort.save(transfer);

        eventPublisher.publishEvent(new TransferInitiated(
                saved.getId().toString(),
                saved.getSourceAccountNumber(),
                saved.getDestinationAccountNumber(),
                saved.getAmount()));

        return toResult(saved);
    }

    // -------------------------------------------------------------------------
    // GetTransferUseCase
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public TransferResult getById(String transferId) {
        return loadTransferPort.findById(TransferId.of(transferId))
                .map(this::toGetResult)
                .orElseThrow(() -> new TransferNotFoundException(transferId));
    }

    // -------------------------------------------------------------------------
    // Event listeners — choreography handlers
    // -------------------------------------------------------------------------

    /**
     * Account-module has successfully debited the source.
     * Advance transfer to DEBITED state.
     * Account-module's TransferInitiatedHandler will now credit destination.
     */
    @EventListener
    public void onMoneyWithdrawn(MoneyWithdrawn event) {
        // Only handle withdrawals that are part of a transfer
        // (reference will be the transferId for transfer-related withdrawals)
        String transferId = event.getReference();
        loadTransferPort.findById(TransferId.of(transferId)).ifPresent(transfer -> {
            try {
                transfer.markDebited();
                saveTransferPort.save(transfer);
                log.info("Transfer {} marked DEBITED", transferId);
            } catch (Exception e) {
                log.error("Failed to mark transfer {} as DEBITED", transferId, e);
            }
        });
    }

    /**
     * Account-module has successfully credited the destination.
     * Advance transfer to COMPLETED.
     */
    @EventListener
    public void onMoneyDeposited(MoneyDeposited event) {
        String transferId = event.getReference();
        loadTransferPort.findById(TransferId.of(transferId)).ifPresent(transfer -> {
            if (!transfer.isDebited())
                return; // deposit unrelated to this transfer

            try {
                transfer.markCompleted();
                saveTransferPort.save(transfer);

                eventPublisher.publishEvent(new TransferCompleted(
                        transfer.getId().toString(),
                        transfer.getSourceAccountNumber(),
                        transfer.getDestinationAccountNumber(),
                        transfer.getAmount()));

                log.info("Transfer {} COMPLETED", transferId);
            } catch (Exception e) {
                log.error("Failed to complete transfer {}", transferId, e);
                handleTransferFailure(transfer, "Failed to mark transfer completed: "
                        + e.getMessage());
            }
        });
    }

    /**
     * Account-module has reversed the source debit.
     * Mark transfer as REVERSED — terminal state.
     */
    @EventListener
    public void onTransferReversed(TransferReversed event) {
        String transferId = event.getAggregateId();
        loadTransferPort.findById(TransferId.of(transferId)).ifPresent(transfer -> {
            transfer.markReversed();
            saveTransferPort.save(transfer);
            log.info("Transfer {} REVERSED", transferId);
        });
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void handleTransferFailure(Transfer transfer, String reason) {
        boolean wasDebited = transfer.isDebited();
        transfer.markFailed(reason);
        saveTransferPort.save(transfer);

        if (wasDebited) {
            // Money left the source — trigger reversal
            eventPublisher.publishEvent(new TransferFailed(
                    transfer.getId().toString(),
                    transfer.getSourceAccountNumber(),
                    transfer.getAmount(),
                    reason));
        }

        log.warn("Transfer {} FAILED: {}", transfer.getId(), reason);
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    private TransferResult toResult(Transfer transfer) {
        return new TransferResult(
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
}