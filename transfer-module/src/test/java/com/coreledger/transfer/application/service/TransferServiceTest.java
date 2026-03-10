// transfer-module/src/test/java/com/coreledger/transfer/application/service/TransferServiceTest.java
package com.coreledger.transfer.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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

import com.coreledger.shared.domain.Currency;
import com.coreledger.shared.domain.Money;
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
 * Unit tests for TransferService.
 *
 * Same pattern as AccountServiceTest: mock all ports, test orchestration only.
 * We are NOT re-testing Transfer state machine logic — TransferTest covers
 * that.
 * We are testing:
 * - Service verifies both accounts exist before initiating
 * - Service verifies currencies match before initiating
 * - Service saves transfer and publishes event
 * - Service throws correct exceptions for missing data
 */
@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private LoadTransferPort loadTransferPort;
    @Mock
    private SaveTransferPort saveTransferPort;
    @Mock
    private AccountVerificationPort accountVerificationPort;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TransferService transferService;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static final String SOURCE = "1234567890";
    private static final String DESTINATION = "0987654321";
    private static final BigDecimal AMOUNT = new BigDecimal("500.00");
    private static final String INITIATED_BY = "test-user";

    // private AccountVerificationPort.AccountView activeNgnAccount(String
    // accountNumber) {
    // return new AccountVerificationPort.AccountView(accountNumber, Currency.NGN,
    // true);
    // }

    private InitiateTransferUseCase.Command validCommand() {
        return new InitiateTransferUseCase.Command(SOURCE, DESTINATION, AMOUNT, INITIATED_BY);
    }

    // mockBothAccountsActive becomes:
    private void mockBothAccountsActive() {
        when(accountVerificationPort.findActiveAccount(SOURCE))
                .thenReturn(new AccountVerificationPort.AccountView(SOURCE, Currency.NGN, true));
        when(accountVerificationPort.findActiveAccount(DESTINATION))
                .thenReturn(new AccountVerificationPort.AccountView(DESTINATION, Currency.NGN, true));
    }

    // ── Initiate transfer ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Initiate transfer")
    class InitiateTransfer {

        @Test
        @DisplayName("should save transfer and return result on valid command")
        void shouldSaveTransferOnValidCommand() {
            mockBothAccountsActive();
            when(saveTransferPort.save(any(Transfer.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            InitiateTransferUseCase.TransferResult result = transferService.execute(validCommand());

            assertThat(result.sourceAccountNumber()).isEqualTo(SOURCE);
            assertThat(result.destinationAccountNumber()).isEqualTo(DESTINATION);
            assertThat(result.amount()).isEqualByComparingTo(AMOUNT);
            assertThat(result.status()).isEqualTo(TransferStatus.INITIATED.name());
        }

        @Test
        @DisplayName("should save transfer with INITIATED status")
        void shouldSaveWithInitiatedStatus() {
            mockBothAccountsActive();
            ArgumentCaptor<Transfer> captor = ArgumentCaptor.forClass(Transfer.class);
            when(saveTransferPort.save(captor.capture()))
                    .thenAnswer(inv -> inv.getArgument(0));

            transferService.execute(validCommand());

            assertThat(captor.getValue().getStatus()).isEqualTo(TransferStatus.INITIATED);
        }

        @Test
        @DisplayName("should call saveTransferPort exactly once")
        void shouldSaveExactlyOnce() {
            mockBothAccountsActive();
            when(saveTransferPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            transferService.execute(validCommand());

            verify(saveTransferPort, times(1)).save(any(Transfer.class));
        }

        @Test
        @DisplayName("should publish TransferInitiated event after saving")
        void shouldPublishEventAfterSaving() {
            mockBothAccountsActive();
            when(saveTransferPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            transferService.execute(validCommand());

            verify(eventPublisher, times(1)).publishEvent(any(Object.class));
        }

        @Test
        @DisplayName("should throw InvalidTransferException when source account not found")
        void shouldThrowWhenSourceAccountNotFound() {
            when(accountVerificationPort.findActiveAccount(SOURCE))
                    .thenThrow(new InvalidTransferException("Source account not found" + SOURCE));

            assertThatThrownBy(() -> transferService.execute(validCommand()))
                    .isInstanceOf(InvalidTransferException.class)
                    .hasMessageContaining(SOURCE);
        }

        @Test
        @DisplayName("should throw InvalidTransferException when destination account not found")
        void shouldThrowWhenDestinationAccountNotFound() {
            when(accountVerificationPort.findActiveAccount(SOURCE))
                    // .thenReturn(Optional.of(activeNgnAccount(SOURCE)));
                    .thenReturn(new AccountVerificationPort.AccountView(SOURCE, Currency.NGN, true));
            when(accountVerificationPort.findActiveAccount(DESTINATION))
                    .thenThrow(new InvalidTransferException("Destination account not found" + DESTINATION));

            assertThatThrownBy(() -> transferService.execute(validCommand()))
                    .isInstanceOf(InvalidTransferException.class)
                    .hasMessageContaining(DESTINATION);
        }

        // @Test
        // @DisplayName("should throw InvalidTransferException when source account is
        // inactive")
        // void shouldThrowWhenSourceAccountInactive() {
        // AccountVerificationPort.AccountView inactive = new
        // AccountVerificationPort.AccountView(SOURCE, Currency.NGN,
        // false);
        // when(accountVerificationPort.findActiveAccount(SOURCE))
        // .thenThrow(new InvalidTransferException("Source Account is inactive"));

        // assertThatThrownBy(() -> transferService.execute(validCommand()))
        // .isInstanceOf(InvalidTransferException.class);
        // }

        @Test
        @DisplayName("should throw InvalidTransferException when currencies differ")
        void shouldThrowWhenCurrenciesDiffer() {
            AccountVerificationPort.AccountView ngnAccount = new AccountVerificationPort.AccountView(SOURCE,
                    Currency.NGN, true);
            AccountVerificationPort.AccountView usdAccount = new AccountVerificationPort.AccountView(DESTINATION,
                    Currency.USD, true);

            when(accountVerificationPort.findActiveAccount(SOURCE))
                    .thenReturn(ngnAccount);
            when(accountVerificationPort.findActiveAccount(DESTINATION))
                    .thenReturn(usdAccount);

            assertThatThrownBy(() -> transferService.execute(validCommand()))
                    .isInstanceOf(InvalidTransferException.class)
                    .hasMessageContaining("currency");
        }

        @Test
        @DisplayName("should not save if account verification fails")
        void shouldNotSaveIfVerificationFails() {
            when(accountVerificationPort.findActiveAccount(anyString()))
                    .thenThrow(new InvalidTransferException(DESTINATION));

            try {
                transferService.execute(validCommand());
            } catch (InvalidTransferException ignored) {
            }

            verify(saveTransferPort, never()).save(any());
        }
    }

    // ── Get transfer ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Get transfer")
    class GetTransfer {

        @Test
        @DisplayName("should return transfer result when transfer exists")
        void shouldReturnTransferWhenExists() {
            Transfer transfer = Transfer.initiate(
                    SOURCE, DESTINATION, Money.of("500.00", Currency.NGN), INITIATED_BY);
            when(loadTransferPort.findById(any(TransferId.class)))
                    .thenReturn(Optional.of(transfer));

            GetTransferUseCase.TransferResult result = transferService.getById(transfer.getId().getValue().toString());

            assertThat(result.sourceAccountNumber()).isEqualTo(SOURCE);
            assertThat(result.destinationAccountNumber()).isEqualTo(DESTINATION);
            assertThat(result.status()).isEqualTo(TransferStatus.INITIATED.name());
        }

        @Test
        @DisplayName("should throw TransferNotFoundException when transfer does not exist")
        void shouldThrowWhenTransferNotFound() {
            TransferId _id = TransferId.generate();
            when(loadTransferPort.findById(_id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transferService.getById(_id.toString()))
                    .isInstanceOf(TransferNotFoundException.class)
                    .hasMessageContaining(_id.toString());
        }
    }
}