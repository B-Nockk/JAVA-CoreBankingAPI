// account-module/src/test/java/com/coreledger/account/application/service/AccountServiceTest.java
package com.coreledger.account.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.coreledger.account.application.port.in.CreateAccountUseCase;
import com.coreledger.account.application.port.in.DepositUseCase;
import com.coreledger.account.application.port.in.GetAccountUseCase;
import com.coreledger.account.application.port.out.AccountNumberGeneratorPort;
import com.coreledger.account.application.port.out.LoadAccountPort;
import com.coreledger.account.application.port.out.SaveAccountPort;
import com.coreledger.account.domain.exceptions.AccountNotFoundException;
import com.coreledger.account.domain.model.Account;
import com.coreledger.shared.domain.Currency;
import com.coreledger.shared.domain.Money;

/**
 * Unit tests for AccountService.
 *
 * Strategy: mock all dependencies, test only the service's orchestration logic.
 * We are NOT testing Account domain logic here — AccountTest does that.
 * We are testing that the service:
 * - calls the right ports in the right order
 * - maps inputs and outputs correctly
 * - throws the right exceptions when ports return nothing
 * - publishes events at the right moments
 *
 * @ExtendWith(MockitoExtension.class) activates Mockito annotations.
 * @Mock creates a mock implementation of the interface.
 * @InjectMocks creates AccountService and injects all @Mock fields into it.
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private LoadAccountPort loadAccountPort;
    @Mock
    private SaveAccountPort saveAccountPort;
    @Mock
    private AccountNumberGeneratorPort accountNumberGenerator;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AccountService accountService;

    // -------------------------------------------------------------------------
    // CreateAccountUseCase
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Create account")
    class CreateAccount {

        @Test
        @DisplayName("should generate account number and save new account")
        void shouldGenerateAccountNumberAndSaveNewAccount() {
            // Arrange
            String generatedNumber = "1234567890";
            when(accountNumberGenerator.generate()).thenReturn(generatedNumber);

            // saveAccountPort returns whatever it receives (simulates DB save)
            when(saveAccountPort.save(any(Account.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CreateAccountUseCase.Command command = new CreateAccountUseCase.Command(
                    "Ada Obi", Currency.NGN, "test-user");

            // Act
            CreateAccountUseCase.AccountCreatedResult result = accountService.execute(command);

            // Assert
            assertThat(result.accountNumber()).isEqualTo(generatedNumber);
            assertThat(result.ownerName()).isEqualTo("Ada Obi");
            assertThat(result.currency()).isEqualTo(Currency.NGN);
            assertThat(result.status()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("should call saveAccountPort exactly once")
        void shouldCallSavePortExactlyOnce() {
            when(accountNumberGenerator.generate()).thenReturn("1234567890");
            when(saveAccountPort.save(any(Account.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            accountService.execute(new CreateAccountUseCase.Command(
                    "Ada Obi", Currency.NGN, "test-user"));

            // verify() asserts the mock was called — times(1) is the default
            verify(saveAccountPort, times(1)).save(any(Account.class));
        }

        @Test
        @DisplayName("should publish AccountCreated event after saving")
        void shouldPublishAccountCreatedEvent() {
            when(accountNumberGenerator.generate()).thenReturn("1234567890");
            when(saveAccountPort.save(any(Account.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            accountService.execute(new CreateAccountUseCase.Command(
                    "Ada Obi", Currency.NGN, "test-user"));

            // verify event was published — we don't care about the specific
            // event content here, just that publishEvent was called once
            // verify(eventPublisher, times(1)).publishEvent(any());
            verify(eventPublisher, times(1)).publishEvent(any(Object.class));

        }

        @Test
        @DisplayName("should save account with correct owner and currency")
        void shouldSaveAccountWithCorrectDetails() {
            when(accountNumberGenerator.generate()).thenReturn("1234567890");

            // ArgumentCaptor captures the actual argument passed to save()
            // so we can assert on its contents
            ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
            when(saveAccountPort.save(accountCaptor.capture()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            accountService.execute(new CreateAccountUseCase.Command(
                    "Chidi Okeke", Currency.USD, "test-user"));

            Account savedAccount = accountCaptor.getValue();
            assertThat(savedAccount.getOwnerName()).isEqualTo("Chidi Okeke");
            assertThat(savedAccount.getCurrency()).isEqualTo(Currency.USD);
            assertThat(savedAccount.getBalance()).isEqualTo(Money.zero(Currency.USD));
        }
    }

    // -------------------------------------------------------------------------
    // GetAccountUseCase
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Get account")
    class GetAccount {

        @Test
        @DisplayName("should return account result when account exists")
        void shouldReturnAccountWhenExists() {
            Account account = Account.open("1234567890", "Ada Obi", Currency.NGN, "test");
            when(loadAccountPort.findByAccountNumber("1234567890"))
                    .thenReturn(Optional.of(account));

            GetAccountUseCase.AccountResult result = accountService.getByAccountNumber("1234567890");

            assertThat(result.accountNumber()).isEqualTo("1234567890");
            assertThat(result.ownerName()).isEqualTo("Ada Obi");
            assertThat(result.currency()).isEqualTo(Currency.NGN);
            assertThat(result.status()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("should throw AccountNotFoundException when account does not exist")
        void shouldThrowWhenAccountDoesNotExist() {
            when(loadAccountPort.findByAccountNumber(anyString()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.getByAccountNumber("9999999999"))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessageContaining("9999999999");
        }

        @Test
        @DisplayName("should return all accounts from load port")
        void shouldReturnAllAccounts() {
            Account a1 = Account.open("1111111111", "Ada Obi", Currency.NGN, "test");
            Account a2 = Account.open("2222222222", "Chidi Okeke", Currency.NGN, "test");
            when(loadAccountPort.findAll()).thenReturn(List.of(a1, a2));

            List<GetAccountUseCase.AccountResult> results = accountService.getAllAccounts();

            assertThat(results).hasSize(2);
            assertThat(results.get(0).accountNumber()).isEqualTo("1111111111");
            assertThat(results.get(1).accountNumber()).isEqualTo("2222222222");
        }

        @Test
        @DisplayName("should return empty list when no accounts exist")
        void shouldReturnEmptyListWhenNoAccountsExist() {
            when(loadAccountPort.findAll()).thenReturn(List.of());

            List<GetAccountUseCase.AccountResult> results = accountService.getAllAccounts();

            assertThat(results).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // DepositUseCase
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Deposit")
    class Deposit {

        @Test
        @DisplayName("should load account, deposit, save and return result")
        void shouldDepositSuccessfully() {
            Account account = Account.open("1234567890", "Ada Obi", Currency.NGN, "test");
            when(loadAccountPort.findByAccountNumber("1234567890"))
                    .thenReturn(Optional.of(account));
            when(saveAccountPort.save(any(Account.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            DepositUseCase.Command command = new DepositUseCase.Command(
                    "1234567890", new BigDecimal("500.00"), "REF001", "test-user");

            DepositUseCase.DepositResult result = accountService.execute(command);

            assertThat(result.accountNumber()).isEqualTo("1234567890");
            assertThat(result.amount()).isEqualTo(Money.of("500.00", Currency.NGN));
            assertThat(result.balanceAfter()).isEqualTo(Money.of("500.00", Currency.NGN));
        }

        @Test
        @DisplayName("should resolve currency from account not from request")
        void shouldResolveCurrencyFromAccount() {
            // Account is USD — deposit sends no currency, service must derive it
            Account account = Account.open("1234567890", "Ada Obi", Currency.USD, "test");
            when(loadAccountPort.findByAccountNumber("1234567890"))
                    .thenReturn(Optional.of(account));
            when(saveAccountPort.save(any(Account.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            DepositUseCase.Command command = new DepositUseCase.Command(
                    "1234567890", new BigDecimal("200.00"), "REF001", "test-user");

            DepositUseCase.DepositResult result = accountService.execute(command);

            // Result should reflect USD not NGN
            assertThat(result.amount().getCurrency()).isEqualTo(Currency.USD);
        }

        @Test
        @DisplayName("should save account after deposit")
        void shouldSaveAccountAfterDeposit() {
            Account account = Account.open("1234567890", "Ada Obi", Currency.NGN, "test");
            when(loadAccountPort.findByAccountNumber("1234567890"))
                    .thenReturn(Optional.of(account));
            when(saveAccountPort.save(any(Account.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            accountService.execute(new DepositUseCase.Command(
                    "1234567890", new BigDecimal("500.00"), "REF001", "test-user"));

            verify(saveAccountPort, times(1)).save(any(Account.class));
        }

        @Test
        @DisplayName("should publish MoneyDeposited event after deposit")
        void shouldPublishEventAfterDeposit() {
            Account account = Account.open("1234567890", "Ada Obi", Currency.NGN, "test");
            when(loadAccountPort.findByAccountNumber("1234567890"))
                    .thenReturn(Optional.of(account));
            when(saveAccountPort.save(any(Account.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            accountService.execute(new DepositUseCase.Command(
                    "1234567890", new BigDecimal("500.00"), "REF001", "test-user"));

            // verify(eventPublisher, times(1)).publishEvent(any());
            verify(eventPublisher, times(1)).publishEvent(any(Object.class));

        }

        @Test
        @DisplayName("should throw AccountNotFoundException when account does not exist")
        void shouldThrowWhenAccountNotFound() {
            when(loadAccountPort.findByAccountNumber(anyString()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.execute(new DepositUseCase.Command(
                    "9999999999", new BigDecimal("500.00"), "REF001", "test-user")))
                    .isInstanceOf(AccountNotFoundException.class);
        }

        @Test
        @DisplayName("should not save if account is not found")
        void shouldNotSaveIfAccountNotFound() {
            when(loadAccountPort.findByAccountNumber(anyString()))
                    .thenReturn(Optional.empty());

            try {
                accountService.execute(new DepositUseCase.Command(
                        "9999999999", new BigDecimal("500.00"), "REF001", "test-user"));
            } catch (AccountNotFoundException ignored) {
            }

            // save should never have been called
            verify(saveAccountPort, never()).save(any());
        }
    }
}