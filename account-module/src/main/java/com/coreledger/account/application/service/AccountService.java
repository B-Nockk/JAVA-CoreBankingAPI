// account-module/src/main/java/com/coreledger/account/application/service/AccountService.java
package com.coreledger.account.application.service;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coreledger.account.application.port.in.CreateAccountUseCase;
import com.coreledger.account.application.port.in.DepositUseCase;
import com.coreledger.account.application.port.in.GetAccountUseCase;
import com.coreledger.account.application.port.out.AccountNumberGeneratorPort;
import com.coreledger.account.application.port.out.LoadAccountPort;
import com.coreledger.account.application.port.out.SaveAccountPort;
import com.coreledger.account.domain.events.AccountCreated;
import com.coreledger.account.domain.events.MoneyDeposited;
import com.coreledger.account.domain.exceptions.AccountNotFoundException;
import com.coreledger.account.domain.model.Account;
import com.coreledger.account.domain.model.Transaction;

/**
 * Application service for the account bounded context.
 *
 * Responsibilities:
 * - Implements all account inbound ports (use cases)
 * - Orchestrates the domain: load → act → save → publish event
 * - Owns transaction boundaries (@Transactional)
 * - Maps domain objects to result records for callers
 *
 * What this service must NOT do:
 * - Contain business logic — that belongs in the domain (Account, Transaction)
 * - Know about HTTP, JSON, or JPA — those are infrastructure concerns
 * - Reach into Transaction internals except through Account methods
 *
 * The pattern here is always the same four steps:
 * 1. Load the aggregate via outbound port
 * 2. Call the behaviour on the aggregate (domain does the work)
 * 3. Persist via outbound port
 * 4. Publish domain event
 * This consistency makes the service easy to read and reason about.
 *
 * ApplicationEventPublisher is Spring's in-process event bus.
 * For now events are handled synchronously within the same transaction.
 * When we add Kafka, the publisher implementation swaps out — this
 * service doesn't change at all.
 */
@Service
@Transactional
public class AccountService implements CreateAccountUseCase, GetAccountUseCase, DepositUseCase {

    private final LoadAccountPort loadAccountPort;
    private final SaveAccountPort saveAccountPort;
    private final AccountNumberGeneratorPort accountNumberGenerator;
    private final ApplicationEventPublisher eventPublisher;

    public AccountService(
            LoadAccountPort loadAccountPort,
            SaveAccountPort saveAccountPort,
            AccountNumberGeneratorPort accountNumberGenerator,
            ApplicationEventPublisher eventPublisher) {
        this.loadAccountPort = loadAccountPort;
        this.saveAccountPort = saveAccountPort;
        this.accountNumberGenerator = accountNumberGenerator;
        this.eventPublisher = eventPublisher;
    }

    // -------------------------------------------------------------------------
    // CreateAccountUseCase
    // -------------------------------------------------------------------------

    @Override
    public AccountCreatedResult execute(CreateAccountUseCase.Command command) {
        String accountNumber = accountNumberGenerator.generate();

        Account account = Account.open(
                accountNumber,
                command.ownerName(),
                command.currency(),
                command.openedBy());

        Account saved = saveAccountPort.save(account);

        eventPublisher.publishEvent(new AccountCreated(
                saved.getId().toString(),
                saved.getAccountNumber(),
                saved.getOwnerName(),
                saved.getCurrency()));

        return new AccountCreatedResult(
                saved.getId().toString(),
                saved.getAccountNumber(),
                saved.getOwnerName(),
                saved.getCurrency(),
                saved.getStatus().name());
    }

    // -------------------------------------------------------------------------
    // GetAccountUseCase
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public AccountResult getByAccountNumber(String accountNumber) {
        Account account = loadAccountPort.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        return toResult(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResult> getAllAccounts() {
        return loadAccountPort.findAll()
                .stream()
                .map(this::toResult)
                .toList();
    }

    // -------------------------------------------------------------------------
    // DepositUseCase
    // -------------------------------------------------------------------------

    @Override
    public DepositResult execute(DepositUseCase.Command command) {
        Account account = loadAccountPort.findByAccountNumber(command.accountNumber())
                .orElseThrow(() -> new AccountNotFoundException(command.accountNumber()));

        Transaction tx = account.deposit(
                command.amount(),
                command.reference(),
                command.initiatedBy());

        saveAccountPort.save(account);

        eventPublisher.publishEvent(new MoneyDeposited(
                account.getId().toString(),
                account.getAccountNumber(),
                tx.getAmount(),
                tx.getBalanceAfter(),
                tx.getReference()));

        return new DepositResult(
                tx.getTransactionId(),
                account.getAccountNumber(),
                tx.getAmount(),
                tx.getBalanceAfter(),
                tx.getAudit().getCreatedAt());
    }

    // -------------------------------------------------------------------------
    // Mapping — domain object → result record
    // -------------------------------------------------------------------------

    private AccountResult toResult(Account account) {
        return new AccountResult(
                account.getId().toString(),
                account.getAccountNumber(),
                account.getOwnerName(),
                account.getCurrency(),
                account.getBalance(),
                account.getStatus().name(),
                account.getAudit().getCreatedAt());
    }
}