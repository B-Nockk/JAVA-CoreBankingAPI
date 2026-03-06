// account-module/src/main/java/com/coreledger/account/infrastructure/persistence/AccountJpaRepository.java
package com.coreledger.account.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA repository for AccountJpaEntity.
 *
 * This is a pure infrastructure interface — it knows about JPA entities,
 * not domain objects. The persistence adapter uses this to talk to the DB
 * and then maps results back to domain objects before returning them.
 *
 * The JOIN FETCH query on findByAccountNumber eagerly loads transactions
 * in a single SQL query, avoiding the N+1 problem.
 *
 * N+1 problem explanation:
 * With LAZY loading, fetching an account and then accessing
 * account.getTransactions()
 * would fire one query for the account and then one MORE query per transaction
 * row.
 * For an account with 500 transactions, that's 501 queries.
 * JOIN FETCH collapses this into a single query with a JOIN — always correct
 * for cases where you know you'll need the transactions.
 */
public interface AccountJpaRepository extends JpaRepository<AccountJpaEntity, UUID> {

    @Query("SELECT a FROM AccountJpaEntity a LEFT JOIN FETCH a.transactions " +
            "WHERE a.accountNumber = :accountNumber")
    Optional<AccountJpaEntity> findByAccountNumberWithTransactions(
            @Param("accountNumber") String accountNumber);

    @Query("SELECT a FROM AccountJpaEntity a LEFT JOIN FETCH a.transactions " +
            "WHERE a.id = :id")
    Optional<AccountJpaEntity> findByIdWithTransactions(@Param("id") UUID id);
}