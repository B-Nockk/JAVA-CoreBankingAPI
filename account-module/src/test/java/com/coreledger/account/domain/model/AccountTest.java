// account-module/src/test/java/com/coreledger/account/domain/model/AccountTest.java
package com.coreledger.account.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.coreledger.shared.domain.Currency;
import com.coreledger.shared.domain.Money;

/**
 * Unit tests for Account aggregate root.
 *
 * Each @Nested class tests one behaviour group.
 *
 * @BeforeEach sets up a fresh account before each test — no shared
 *             mutable state between tests. Tests must be fully independent:
 *             running them in any order must produce the same result.
 */
class AccountTest {

    // Reusable test fixtures
    private static final String ACCOUNT_NUMBER = "1234567890";
    private static final String OWNER = "Ada Obi";
    private static final Currency CURRENCY = Currency.NGN;
    private static final String INITIATED_BY = "test-user";

    private Account account;

    /**
     * @BeforeEach runs before every single @Test method.
     *             Each test gets a fresh ACTIVE account with zero balance.
     */
    @BeforeEach
    void setUp() {
        account = Account.open(ACCOUNT_NUMBER, OWNER, CURRENCY, INITIATED_BY);
    }

    // -------------------------------------------------------------------------
    // Opening an account
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Opening an account")
    class Opening {

        @Test
        @DisplayName("should create account with ACTIVE status")
        void shouldCreateAccountWithActiveStatus() {
            assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        }

        @Test
        @DisplayName("should create account with zero balance")
        void shouldCreateAccountWithZeroBalance() {
            assertThat(account.getBalance()).isEqualTo(Money.zero(CURRENCY));
        }

        @Test
        @DisplayName("should store owner name and currency")
        void shouldStoreOwnerNameAndCurrency() {
            assertThat(account.getOwnerName()).isEqualTo(OWNER);
            assertThat(account.getCurrency()).isEqualTo(CURRENCY);
            assertThat(account.getAccountNumber()).isEqualTo(ACCOUNT_NUMBER);
        }

        @Test
        @DisplayName("should start with empty transaction list")
        void shouldStartWithEmptyTransactionList() {
            assertThat(account.getTransactions()).isEmpty();
        }

        @Test
        @DisplayName("should generate a unique account ID")
        void shouldGenerateUniqueAccountId() {
            Account another = Account.open("0987654321", OWNER, CURRENCY, INITIATED_BY);

            assertThat(account.getId()).isNotNull();
            assertThat(account.getId()).isNotEqualTo(another.getId());
        }
    }

    // -------------------------------------------------------------------------
    // Deposits
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Deposits")
    class Deposits {

        @Test
        @DisplayName("should increase balance after deposit")
        void shouldIncreaseBalanceAfterDeposit() {
            Money amount = Money.of("500.00", CURRENCY);

            account.deposit(amount, "REF001", INITIATED_BY);

            assertThat(account.getBalance()).isEqualTo(Money.of("500.00", CURRENCY));
        }

        @Test
        @DisplayName("should record transaction after deposit")
        void shouldRecordTransactionAfterDeposit() {
            Money amount = Money.of("500.00", CURRENCY);

            account.deposit(amount, "REF001", INITIATED_BY);

            assertThat(account.getTransactions()).hasSize(1);
            Transaction tx = account.getTransactions().get(0);
            assertThat(tx.getType()).isEqualTo(TransactionType.DEPOSIT);
            assertThat(tx.getAmount()).isEqualTo(amount);
            assertThat(tx.getBalanceAfter()).isEqualTo(Money.of("500.00", CURRENCY));
        }

        @Test
        @DisplayName("should accumulate balance across multiple deposits")
        void shouldAccumulateBalanceAcrossMultipleDeposits() {
            account.deposit(Money.of("500.00", CURRENCY), "REF001", INITIATED_BY);
            account.deposit(Money.of("300.00", CURRENCY), "REF002", INITIATED_BY);

            assertThat(account.getBalance()).isEqualTo(Money.of("800.00", CURRENCY));
            assertThat(account.getTransactions()).hasSize(2);
        }

        @Test
        @DisplayName("should allow deposit on FROZEN account")
        void shouldAllowDepositOnFrozenAccount() {
            account.freeze(INITIATED_BY);

            // Deposits (credits) are allowed on frozen accounts — only debits are blocked
            account.deposit(Money.of("100.00", CURRENCY), "REF001", INITIATED_BY);

            assertThat(account.getBalance()).isEqualTo(Money.of("100.00", CURRENCY));
        }

        @Test
        @DisplayName("should reject deposit on CLOSED account")
        void shouldRejectDepositOnClosedAccount() {
            // Close requires zero balance — account starts at zero so this is valid
            account.close(INITIATED_BY);

            assertThatThrownBy(() -> account.deposit(Money.of("100.00", CURRENCY), "REF001", INITIATED_BY))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("should reject deposit with wrong currency")
        void shouldRejectDepositWithWrongCurrency() {
            Money usd = Money.of("100.00", Currency.USD);

            assertThatThrownBy(() -> account.deposit(usd, "REF001", INITIATED_BY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Currency mismatch");
        }

        @Test
        @DisplayName("should reject deposit with zero amount")
        void shouldRejectDepositWithZeroAmount() {
            Money zero = Money.of("0.00", CURRENCY);

            assertThatThrownBy(() -> account.deposit(zero, "REF001", INITIATED_BY))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject deposit with negative amount")
        void shouldRejectDepositWithNegativeAmount() {
            Money negative = Money.of("-100.00", CURRENCY);

            assertThatThrownBy(() -> account.deposit(negative, "REF001", INITIATED_BY))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -------------------------------------------------------------------------
    // Withdrawals
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Withdrawals")
    class Withdrawals {

        @BeforeEach
        void depositFirst() {
            // Give the account funds to work with
            account.deposit(Money.of("1000.00", CURRENCY), "SETUP", INITIATED_BY);
        }

        @Test
        @DisplayName("should decrease balance after withdrawal")
        void shouldDecreaseBalanceAfterWithdrawal() {
            account.withdraw(Money.of("300.00", CURRENCY), "REF001", INITIATED_BY);

            assertThat(account.getBalance()).isEqualTo(Money.of("700.00", CURRENCY));
        }

        @Test
        @DisplayName("should record WITHDRAWAL transaction type")
        void shouldRecordWithdrawalTransactionType() {
            account.withdraw(Money.of("300.00", CURRENCY), "REF001", INITIATED_BY);

            // index 0 is the setup deposit, index 1 is the withdrawal
            Transaction tx = account.getTransactions().get(1);
            assertThat(tx.getType()).isEqualTo(TransactionType.WITHDRAWAL);
        }

        @Test
        @DisplayName("should reject withdrawal exceeding balance")
        void shouldRejectWithdrawalExceedingBalance() {
            Money tooMuch = Money.of("1500.00", CURRENCY);

            assertThatThrownBy(() -> account.withdraw(tooMuch, "REF001", INITIATED_BY))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Insufficient funds");
        }

        @Test
        @DisplayName("should allow withdrawal of exact balance")
        void shouldAllowWithdrawalOfExactBalance() {
            account.withdraw(Money.of("1000.00", CURRENCY), "REF001", INITIATED_BY);

            assertThat(account.getBalance()).isEqualTo(Money.zero(CURRENCY));
        }

        @Test
        @DisplayName("should reject withdrawal on FROZEN account")
        void shouldRejectWithdrawalOnFrozenAccount() {
            account.freeze(INITIATED_BY);

            assertThatThrownBy(() -> account.withdraw(Money.of("100.00", CURRENCY), "REF001", INITIATED_BY))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("should reject withdrawal on CLOSED account")
        void shouldRejectWithdrawalOnClosedAccount() {
            // Drain first so we can close
            account.withdraw(Money.of("1000.00", CURRENCY), "DRAIN", INITIATED_BY);
            account.close(INITIATED_BY);

            assertThatThrownBy(() -> account.withdraw(Money.of("1.00", CURRENCY), "REF001", INITIATED_BY))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // -------------------------------------------------------------------------
    // Balance derivation
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Balance derivation")
    class BalanceDerivation {

        @Test
        @DisplayName("should derive balance from last transaction balanceAfter")
        void shouldDeriveBalanceFromLastTransaction() {
            account.deposit(Money.of("500.00", CURRENCY), "REF001", INITIATED_BY);
            account.deposit(Money.of("200.00", CURRENCY), "REF002", INITIATED_BY);
            account.withdraw(Money.of("100.00", CURRENCY), "REF003", INITIATED_BY);

            // 500 + 200 - 100 = 600
            assertThat(account.getBalance()).isEqualTo(Money.of("600.00", CURRENCY));
        }

        @Test
        @DisplayName("each transaction should record correct running balance")
        void eachTransactionShouldRecordCorrectRunningBalance() {
            account.deposit(Money.of("500.00", CURRENCY), "REF001", INITIATED_BY);
            account.deposit(Money.of("300.00", CURRENCY), "REF002", INITIATED_BY);

            assertThat(account.getTransactions().get(0).getBalanceAfter())
                    .isEqualTo(Money.of("500.00", CURRENCY));
            assertThat(account.getTransactions().get(1).getBalanceAfter())
                    .isEqualTo(Money.of("800.00", CURRENCY));
        }
    }

    // -------------------------------------------------------------------------
    // Account lifecycle (freeze / close)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Account lifecycle")
    class Lifecycle {

        @Test
        @DisplayName("should freeze an ACTIVE account")
        void shouldFreezeActiveAccount() {
            account.freeze(INITIATED_BY);

            assertThat(account.getStatus()).isEqualTo(AccountStatus.FROZEN);
        }

        @Test
        @DisplayName("should unfreeze a FROZEN account back to ACTIVE")
        void shouldUnfreezeAccount() {
            account.freeze(INITIATED_BY);
            account.unfreeze(INITIATED_BY);

            assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        }

        @Test
        @DisplayName("should reject unfreeze on non-FROZEN account")
        void shouldRejectUnfreezeOnNonFrozenAccount() {
            assertThatThrownBy(() -> account.unfreeze(INITIATED_BY))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not frozen");
        }

        @Test
        @DisplayName("should close account with zero balance")
        void shouldCloseAccountWithZeroBalance() {
            account.close(INITIATED_BY);

            assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
        }

        @Test
        @DisplayName("should reject closing account with positive balance")
        void shouldRejectClosingAccountWithPositiveBalance() {
            account.deposit(Money.of("100.00", CURRENCY), "REF001", INITIATED_BY);

            assertThatThrownBy(() -> account.close(INITIATED_BY))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("positive balance");
        }

        @Test
        @DisplayName("should reject freezing a CLOSED account")
        void shouldRejectFreezingClosedAccount() {
            account.close(INITIATED_BY);

            assertThatThrownBy(() -> account.freeze(INITIATED_BY))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("should reject double-closing an account")
        void shouldRejectDoubleClosing() {
            account.close(INITIATED_BY);

            assertThatThrownBy(() -> account.close(INITIATED_BY))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already closed");
        }
    }

    // -------------------------------------------------------------------------
    // Transaction list immutability
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Transaction list immutability")
    class TransactionListImmutability {

        @Test
        @DisplayName("should not allow external modification of transaction list")
        void shouldNotAllowExternalModificationOfTransactionList() {
            account.deposit(Money.of("100.00", CURRENCY), "REF001", INITIATED_BY);

            assertThatThrownBy(() -> account.getTransactions().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}

// RUN Command
// ./mvnw test -pl account-module -Dtest=AccountTest