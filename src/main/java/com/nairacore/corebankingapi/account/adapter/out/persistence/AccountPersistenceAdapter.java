package com.nairacore.corebankingapi.account.adapter.out.persistence;

import com.nairacore.corebankingapi.account.application.port.out.CreateAccountPort;
import com.nairacore.corebankingapi.account.application.port.out.LoadAccountPort;
import com.nairacore.corebankingapi.account.application.port.out.UpdateAccountStatePort;
import com.nairacore.corebankingapi.account.domain.Account;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository // Tells Spring to manage this class
public class AccountPersistenceAdapter implements LoadAccountPort, UpdateAccountStatePort, CreateAccountPort {
    private final AccountRepository repository;

    // Constructor injection (Spring automatically passes the repository in)
    public AccountPersistenceAdapter(AccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public void insert(Account account) {
        AccountJpaEntity entity = new AccountJpaEntity();

        // This is a strict insert. We don't fetch from the DB first.
        entity.setAccountNumber(account.getAccountNumber());
        entity.setBalance(account.getBalance());
        entity.setStatus(account.getStatus().name());
        entity.setVersion(account.getVersion()); // Will be 0 from the factory

        repository.save(entity);
    }

    @Override
    public void save(Account account) {
        // 1. Fetch the exact row from the database (so we have the DB ID)
        AccountJpaEntity entity = repository.findByAccountNumber(
                account.getAccountNumber())
                .orElseThrow(() -> new IllegalStateException("Account must exist to be updated"));

        // 2. Update the fields that might have changed
        entity.setBalance(account.getBalance());
        entity.setStatus(account.getStatus().name());
        entity.setVersion(account.getVersion()); // Pass the domain version back

        // 3. Save. If someone else changed the DB version in the meantime,
        // Hibernate throws ObjectOptimisticLockingFailureException!
        if (account.getVersion() != null) {
            entity.setVersion(account.getVersion());
        }
        repository.save(entity);
    }

    @Override
    public Optional<Account> loadAccount(String accountNumber) {
        return repository.findByAccountNumber(accountNumber)
                .map(entity -> new Account(
                        entity.getAccountNumber(),
                        entity.getBalance(),
                        entity.getVersion() // Load the version from DB
                ));
    }

}