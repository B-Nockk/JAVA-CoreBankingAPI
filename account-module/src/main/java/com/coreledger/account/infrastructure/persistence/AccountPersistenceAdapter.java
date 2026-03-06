// account-module/src/main/java/com/coreledger/account/infrastructure/persistence/AccountPersistenceAdapter.java
package com.coreledger.account.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.coreledger.account.application.port.out.LoadAccountPort;
import com.coreledger.account.application.port.out.SaveAccountPort;
import com.coreledger.account.domain.model.Account;
import com.coreledger.account.domain.model.AccountId;
import com.coreledger.account.domain.model.Transaction;
import com.coreledger.shared.domain.AuditMetadata;
import com.coreledger.shared.domain.Currency;
import com.coreledger.shared.domain.Money;

/**
 * Persistence adapter for the account bounded context.
 *
 * Implements both LoadAccountPort and SaveAccountPort — it is the only
 * class that knows how to translate between domain objects and JPA entities.
 *
 * This class has two mapping directions:
 * - toDomain(): JPA entity → domain object (used when loading)
 * - toJpaEntity(): domain object → JPA entity (used when saving)
 *
 * The mapping is intentionally explicit (no MapStruct here) so you can
 * see exactly what maps to what. In a larger project you would introduce
 * a mapping library, but explicit mapping is better for learning and
 * easier to debug when something looks wrong in the DB.
 *
 * @Component marks this as a Spring bean — it will be injected wherever
 *            LoadAccountPort or SaveAccountPort is required.
 */
@Component
public class AccountPersistenceAdapter implements LoadAccountPort, SaveAccountPort {

    private final AccountJpaRepository accountJpaRepository;

    public AccountPersistenceAdapter(AccountJpaRepository accountJpaRepository) {
        this.accountJpaRepository = accountJpaRepository;
    }

    // -------------------------------------------------------------------------
    // LoadAccountPort
    // -------------------------------------------------------------------------

    @Override
    public Optional<Account> findById(AccountId id) {
        return accountJpaRepository
                .findByIdWithTransactions(id.getValue())
                .map(this::toDomain);
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        return accountJpaRepository
                .findByAccountNumberWithTransactions(accountNumber)
                .map(this::toDomain);
    }

    @Override
    public List<Account> findAll() {
        // findAll() from JpaRepository — transactions loaded lazily here.
        // For the list view we don't need full transaction history,
        // only account metadata + derived balance from the last transaction.
        // A future optimisation: native query fetching only the last transaction per
        // account.
        return accountJpaRepository.findAll()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    // -------------------------------------------------------------------------
    // SaveAccountPort
    // -------------------------------------------------------------------------

    @Override
    public Account save(Account account) {
        AccountJpaEntity entity = toJpaEntity(account);
        AccountJpaEntity saved = accountJpaRepository.save(entity);
        return toDomain(saved);
    }

    // -------------------------------------------------------------------------
    // Mapping: JPA entity → domain object
    // -------------------------------------------------------------------------

    private Account toDomain(AccountJpaEntity entity) {
        List<Transaction> transactions = entity.getTransactions()
                .stream()
                .map(tx -> toDomainTransaction(tx))
                .toList();

        return Account.reconstitute(
                AccountId.of(entity.getId()),
                entity.getAccountNumber(),
                entity.getOwnerName(),
                entity.getCurrency(),
                entity.getStatus(),
                transactions,
                AuditMetadata.of(entity.getCreatedAt(), entity.getCreatedBy()));
    }

    private Transaction toDomainTransaction(TransactionJpaEntity tx) {
        return Transaction.reconstitute(
                tx.getId().toString(),
                AccountId.of(tx.getAccount().getId()),
                tx.getType(),
                Money.of(tx.getAmount(), Currency.valueOf(tx.getCurrency())),
                Money.of(tx.getBalanceAfter(), Currency.valueOf(tx.getCurrency())),
                tx.getReference(),
                AuditMetadata.of(tx.getCreatedAt(), tx.getCreatedBy()));
    }

    // -------------------------------------------------------------------------
    // Mapping: domain object → JPA entity
    // -------------------------------------------------------------------------

    private AccountJpaEntity toJpaEntity(Account account) {
        AccountJpaEntity entity = new AccountJpaEntity();
        entity.setId(account.getId().getValue());
        entity.setAccountNumber(account.getAccountNumber());
        entity.setOwnerName(account.getOwnerName());
        entity.setCurrency(account.getCurrency());
        entity.setStatus(account.getStatus());
        entity.setCreatedAt(account.getAudit().getCreatedAt());
        entity.setCreatedBy(account.getAudit().getCreatedBy());

        List<TransactionJpaEntity> txEntities = account.getTransactions()
                .stream()
                .map(tx -> toJpaTransaction(tx, entity))
                .toList();

        entity.getTransactions().clear();
        entity.getTransactions().addAll(txEntities);

        return entity;
    }

    private TransactionJpaEntity toJpaTransaction(Transaction tx, AccountJpaEntity accountEntity) {
        TransactionJpaEntity entity = new TransactionJpaEntity();
        entity.setId(UUID.fromString(tx.getTransactionId()));
        entity.setAccount(accountEntity);
        entity.setType(tx.getType());
        entity.setAmount(tx.getAmount().getAmount());
        entity.setBalanceAfter(tx.getBalanceAfter().getAmount());
        entity.setCurrency(tx.getAmount().getCurrency().name());
        entity.setReference(tx.getReference());
        entity.setCreatedAt(tx.getAudit().getCreatedAt());
        entity.setCreatedBy(tx.getAudit().getCreatedBy());
        return entity;
    }
}