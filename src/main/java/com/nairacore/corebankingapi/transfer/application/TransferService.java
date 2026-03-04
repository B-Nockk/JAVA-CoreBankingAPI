// src/main/java/com/nairacore/corebankingapi/transfer/application/TransferService.java
package com.nairacore.corebankingapi.transfer.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nairacore.corebankingapi.account.application.port.out.LoadAccountPort;
import com.nairacore.corebankingapi.account.application.port.out.UpdateAccountStatePort;
import com.nairacore.corebankingapi.account.domain.Account;
import com.nairacore.corebankingapi.common.config.BankingProperties;
import com.nairacore.corebankingapi.transfer.application.port.in.InitiateTransferUseCase;
import com.nairacore.corebankingapi.transfer.adapter.out.persistence.TransferRepositoryPort;
import com.nairacore.corebankingapi.transfer.domain.Transfer;

@Service
public class TransferService implements InitiateTransferUseCase {

    private final LoadAccountPort loadAccountPort;
    private final UpdateAccountStatePort updateAccountStatePort;
    private final TransferRepositoryPort transferRepositoryPort;
    private final BankingProperties bankingProperties;
    private final Clock clock;

    public TransferService(
            LoadAccountPort loadAccountPort,
            UpdateAccountStatePort updateAccountStatePort,
            TransferRepositoryPort transferRepositoryPort,
            BankingProperties bankingProperties,
            Clock clock) {
        this.loadAccountPort = loadAccountPort;
        this.updateAccountStatePort = updateAccountStatePort;
        this.transferRepositoryPort = transferRepositoryPort;
        this.bankingProperties = bankingProperties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Transfer initiateTransfer(
            BigDecimal amount,
            String recipientAccountNumber,
            String senderAccountNumber) {

        Instant now = Instant.now(clock);

        // 1️⃣ Create initial PENDING transfer
        Transfer pendingTransfer = Transfer.initiate(
                senderAccountNumber,
                recipientAccountNumber,
                amount,
                now);

        // 2️⃣ Append initial state
        transferRepositoryPort.append(pendingTransfer);

        try {

            // 3️⃣ Load accounts
            Account sender = loadAccountPort.loadAccount(senderAccountNumber)
                    .orElseThrow(() -> new IllegalArgumentException("Sender account not found"));

            Account recipient = loadAccountPort.loadAccount(recipientAccountNumber)
                    .orElseThrow(() -> new IllegalArgumentException("Recipient account not found"));

            // 4️⃣ Domain rules
            sender.withdraw(amount, bankingProperties.minimumBalance());
            recipient.deposit(amount);

            // 5️⃣ Persist account state
            updateAccountStatePort.save(sender);
            updateAccountStatePort.save(recipient);

            // 6️⃣ Transition transfer → COMPLETED
            Transfer completedTransfer = pendingTransfer.complete(now);

            // 7️⃣ Append new state (no update, no overwrite)
            transferRepositoryPort.append(completedTransfer);

            return completedTransfer;

        } catch (Exception e) {

            // 8️⃣ Transition transfer → FAILED
            Transfer failedTransfer = pendingTransfer.fail();

            transferRepositoryPort.append(failedTransfer);

            throw e; // triggers rollback
        }
    }
}