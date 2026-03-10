// account-module/src/test/java/com/coreledger/account/infrastructure/persistence/AccountPersistenceAdapterTest.java
package com.coreledger.account.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.coreledger.account.domain.model.Account;
import com.coreledger.account.domain.model.AccountStatus;
import com.coreledger.account.domain.model.TransactionType;
import com.coreledger.shared.domain.Currency;
import com.coreledger.shared.domain.Money;

/**
 * Integration tests for AccountPersistenceAdapter.
 *
 * @DataJpaTest: spins up only the JPA slice of Spring.
 *               Uses H2 in-memory database — no Postgres needed.
 *               Schema is created automatically from your JPA entities
 *               (ddl-auto=create-drop).
 *               Each test runs in a transaction that is rolled back after — DB
 *               resets automatically.
 *
 *               @Import(AccountPersistenceAdapter.class): our adapter isn't
 *               a @Repository so
 * @DataJpaTest won't pick it up automatically. We import it explicitly.
 *
 *              @ActiveProfiles("test"): activates the test profile in
 *              application.yml
 *              which points to H2 instead of Postgres.
 */
@DataJpaTest
@Import(AccountPersistenceAdapter.class)
@ActiveProfiles("test")
class AccountPersistenceAdapterTest {

    // Spring injects the real adapter wired with the real JPA repository
    @Autowired
    private AccountPersistenceAdapter adapter;

    // We also autowire the repository directly so we can set up test data
    // without going through the adapter (keeps tests independent)
    @Autowired
    // private AccountJpaRepository accountJpaRepository;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Account openAndSave(String accountNumber, String owner, Currency currency) {
        Account account = Account.open(accountNumber, owner, currency, "test-user");
        return adapter.save(account);
    }

    // -------------------------------------------------------------------------
    // Save
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Save account")
    class SaveAccount {

        @Test
        @DisplayName("should persist account and return saved domain object")
        void shouldPersistAccount() {
            Account saved = openAndSave("1234567890", "Ada Obi", Currency.NGN);

            assertThat(saved.getAccountNumber()).isEqualTo("1234567890");
            assertThat(saved.getOwnerName()).isEqualTo("Ada Obi");
            assertThat(saved.getCurrency()).isEqualTo(Currency.NGN);
            assertThat(saved.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        }

        @Test
        @DisplayName("should persist account with zero balance")
        void shouldPersistAccountWithZeroBalance() {
            Account saved = openAndSave("1234567890", "Ada Obi", Currency.NGN);

            assertThat(saved.getBalance()).isEqualTo(Money.zero(Currency.NGN));
        }

        @Test
        @DisplayName("should persist account with transactions and correct balance")
        void shouldPersistAccountWithTransactions() {
            Account account = Account.open("1234567890", "Ada Obi", Currency.NGN, "test");
            account.deposit(Money.of("500.00", Currency.NGN), "REF001", "test-user");
            account.deposit(Money.of("200.00", Currency.NGN), "REF002", "test-user");
            adapter.save(account);

            // Reload from DB to confirm transactions were actually persisted
            Account reloaded = adapter.findByAccountNumber("1234567890").get();
            assertThat(reloaded.getTransactions()).hasSize(2);
            assertThat(reloaded.getBalance()).isEqualTo(Money.of("700.00", Currency.NGN));
        }

        @Test
        @DisplayName("should persist transaction types correctly")
        void shouldPersistTransactionTypes() {
            Account account = Account.open("1234567890", "Ada Obi", Currency.NGN, "test");
            account.deposit(Money.of("500.00", Currency.NGN), "REF001", "test-user");
            account.withdraw(Money.of("200.00", Currency.NGN), "REF002", "test-user");
            adapter.save(account);

            Account reloaded = adapter.findByAccountNumber("1234567890").get();
            assertThat(reloaded.getTransactions().get(0).getType()).isEqualTo(TransactionType.DEPOSIT);
            assertThat(reloaded.getTransactions().get(1).getType()).isEqualTo(TransactionType.WITHDRAWAL);
        }

        @Test
        @DisplayName("should persist correct running balance across multiple transactions")
        void shouldPersistRunningBalance() {
            Account account = Account.open("1234567890", "Ada Obi", Currency.NGN, "test");
            account.deposit(Money.of("1000.00", Currency.NGN), "REF001", "test-user");
            account.withdraw(Money.of("300.00", Currency.NGN), "REF002", "test-user");
            account.deposit(Money.of("150.00", Currency.NGN), "REF003", "test-user");
            adapter.save(account);

            Account reloaded = adapter.findByAccountNumber("1234567890").get();
            // 1000 - 300 + 150 = 850
            assertThat(reloaded.getBalance()).isEqualTo(Money.of("850.00", Currency.NGN));
        }
    }

    // -------------------------------------------------------------------------
    // Find by account number
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Find by account number")
    class FindByAccountNumber {

        @Test
        @DisplayName("should return account when it exists")
        void shouldReturnAccountWhenExists() {
            openAndSave("1234567890", "Ada Obi", Currency.NGN);

            Optional<Account> result = adapter.findByAccountNumber("1234567890");

            assertThat(result).isPresent();
            assertThat(result.get().getAccountNumber()).isEqualTo("1234567890");
        }

        @Test
        @DisplayName("should return empty Optional when account does not exist")
        void shouldReturnEmptyWhenNotFound() {
            Optional<Account> result = adapter.findByAccountNumber("9999999999");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should load transactions with account — JOIN FETCH working")
        void shouldLoadTransactionsWithAccount() {
            Account account = Account.open("1234567890", "Ada Obi", Currency.NGN, "test");
            account.deposit(Money.of("100.00", Currency.NGN), "REF001", "test-user");
            account.deposit(Money.of("200.00", Currency.NGN), "REF002", "test-user");
            adapter.save(account);

            // If JOIN FETCH is working, transactions come back in one query
            // If it's broken, we get LazyInitializationException here
            Account loaded = adapter.findByAccountNumber("1234567890").get();
            assertThat(loaded.getTransactions()).hasSize(2);
        }

        @Test
        @DisplayName("should load transactions in chronological order")
        void shouldLoadTransactionsInChronologicalOrder() {
            Account account = Account.open("1234567890", "Ada Obi", Currency.NGN, "test");
            account.deposit(Money.of("100.00", Currency.NGN), "REF001", "test-user");
            account.deposit(Money.of("200.00", Currency.NGN), "REF002", "test-user");
            account.withdraw(Money.of("50.00", Currency.NGN), "REF003", "test-user");
            adapter.save(account);

            Account loaded = adapter.findByAccountNumber("1234567890").get();
            assertThat(loaded.getTransactions().get(0).getType()).isEqualTo(TransactionType.DEPOSIT);
            assertThat(loaded.getTransactions().get(1).getType()).isEqualTo(TransactionType.DEPOSIT);
            assertThat(loaded.getTransactions().get(2).getType()).isEqualTo(TransactionType.WITHDRAWAL);
        }
    }

    // -------------------------------------------------------------------------
    // Find by ID
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Find by ID")
    class FindById {

        @Test
        @DisplayName("should return account when ID exists")
        void shouldReturnAccountById() {
            // Use the ID from a saved account — never construct AccountId directly
            // (constructor is package-private by design)
            Account saved = openAndSave("1234567890", "Ada Obi", Currency.NGN);

            Optional<Account> result = adapter.findById(saved.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getAccountNumber()).isEqualTo("1234567890");
        }

        @Test
        @DisplayName("should return empty for unknown account number")
        void shouldReturnEmptyForUnknownAccountNumber() {
            Optional<Account> result = adapter.findByAccountNumber("0000000000");

            assertThat(result).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // Find all
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Find all accounts")
    class FindAll {

        @Test
        @DisplayName("should return all persisted accounts")
        void shouldReturnAllAccounts() {
            openAndSave("1111111111", "Ada Obi", Currency.NGN);
            openAndSave("2222222222", "Chidi Okeke", Currency.NGN);
            openAndSave("3333333333", "Ngozi Adeyemi", Currency.USD);

            assertThat(adapter.findAll()).hasSize(3);
        }

        @Test
        @DisplayName("should return empty list when no accounts exist")
        void shouldReturnEmptyListWhenNoAccounts() {
            assertThat(adapter.findAll()).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // Domain mapping round-trip
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Domain mapping round-trip")
    class MappingRoundTrip {

        @Test
        @DisplayName("should preserve all fields through save and reload")
        void shouldPreserveAllFieldsThroughSaveAndReload() {
            Account original = Account.open("1234567890", "Ada Obi", Currency.NGN, "test-user");
            original.deposit(Money.of("500.00", Currency.NGN), "REF001", "test-user");
            original.freeze("admin");
            adapter.save(original);

            Account reloaded = adapter.findByAccountNumber("1234567890").get();

            assertThat(reloaded.getAccountNumber()).isEqualTo("1234567890");
            assertThat(reloaded.getOwnerName()).isEqualTo("Ada Obi");
            assertThat(reloaded.getCurrency()).isEqualTo(Currency.NGN);
            assertThat(reloaded.getStatus()).isEqualTo(AccountStatus.FROZEN);
            assertThat(reloaded.getBalance()).isEqualTo(Money.of("500.00", Currency.NGN));
            assertThat(reloaded.getTransactions()).hasSize(1);
        }

        @Test
        @DisplayName("should preserve transaction reference through save and reload")
        void shouldPreserveTransactionReference() {
            Account account = Account.open("1234567890", "Ada Obi", Currency.NGN, "test");
            account.deposit(Money.of("300.00", Currency.NGN), "REF-XYZ-001", "operator-1");
            adapter.save(account);

            Account reloaded = adapter.findByAccountNumber("1234567890").get();
            assertThat(reloaded.getTransactions().get(0).getReference()).isEqualTo("REF-XYZ-001");
        }
    }
}