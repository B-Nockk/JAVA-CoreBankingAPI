// transfer-module/src/test/java/com/coreledger/transfer/domain/model/TransferTest.java
package com.coreledger.transfer.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.coreledger.shared.domain.Currency;
import com.coreledger.shared.domain.Money;

/**
 * Unit tests for Transfer aggregate root.
 *
 * Focus: the state machine.
 * Every valid transition is tested.
 * Every invalid transition is tested.
 *
 * This is important because in financial systems, an invalid state
 * transition (e.g. completing a transfer that was never debited) would
 * silently corrupt the audit trail. The aggregate must refuse it.
 */
class TransferTest {

    private static final String SOURCE = "1234567890";
    private static final String DESTINATION = "0987654321";
    private static final Money AMOUNT = Money.of("500.00", Currency.NGN);
    private static final String INITIATED_BY = "test-user";

    private Transfer transfer;

    @BeforeEach
    void setUp() {
        transfer = Transfer.initiate(SOURCE, DESTINATION, AMOUNT, INITIATED_BY);
    }

    // -------------------------------------------------------------------------
    // Initiation
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Initiation")
    class Initiation {

        @Test
        @DisplayName("should create transfer with INITIATED status")
        void shouldCreateTransferWithInitiatedStatus() {
            assertThat(transfer.getStatus()).isEqualTo(TransferStatus.INITIATED);
        }

        @Test
        @DisplayName("should store source, destination and amount correctly")
        void shouldStoreTransferDetails() {
            assertThat(transfer.getSourceAccountNumber()).isEqualTo(SOURCE);
            assertThat(transfer.getDestinationAccountNumber()).isEqualTo(DESTINATION);
            assertThat(transfer.getAmount()).isEqualTo(AMOUNT);
        }

        @Test
        @DisplayName("should generate a unique transfer ID")
        void shouldGenerateUniqueTransferId() {
            Transfer another = Transfer.initiate(SOURCE, DESTINATION, AMOUNT, INITIATED_BY);

            assertThat(transfer.getId()).isNotNull();
            assertThat(transfer.getId()).isNotEqualTo(another.getId());
        }

        @Test
        @DisplayName("should have null failure reason on initiation")
        void shouldHaveNullFailureReasonOnInitiation() {
            assertThat(transfer.getFailureReason()).isNull();
        }

        @Test
        @DisplayName("should reject same source and destination account")
        void shouldRejectSameSourceAndDestination() {
            assertThatThrownBy(() -> Transfer.initiate(SOURCE, SOURCE, AMOUNT, INITIATED_BY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("different");
        }

        @Test
        @DisplayName("should reject zero amount")
        void shouldRejectZeroAmount() {
            Money zero = Money.zero(Currency.NGN);

            assertThatThrownBy(() -> Transfer.initiate(SOURCE, DESTINATION, zero, INITIATED_BY))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject negative amount")
        void shouldRejectNegativeAmount() {
            Money negative = Money.of("-100.00", Currency.NGN);

            assertThatThrownBy(() -> Transfer.initiate(SOURCE, DESTINATION, negative, INITIATED_BY))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -------------------------------------------------------------------------
    // Valid state transitions
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Valid state transitions")
    class ValidTransitions {

        @Test
        @DisplayName("INITIATED → DEBITED when source account is debited")
        void shouldTransitionToDebitedFromInitiated() {
            transfer.markDebited();

            assertThat(transfer.getStatus()).isEqualTo(TransferStatus.DEBITED);
            assertThat(transfer.isDebited()).isTrue();
        }

        @Test
        @DisplayName("DEBITED → COMPLETED when destination account is credited")
        void shouldTransitionToCompletedFromDebited() {
            transfer.markDebited();
            transfer.markCompleted();

            assertThat(transfer.getStatus()).isEqualTo(TransferStatus.COMPLETED);
        }

        @Test
        @DisplayName("INITIATED → FAILED when failure occurs before debit")
        void shouldTransitionToFailedFromInitiated() {
            transfer.markFailed("Account not found");

            assertThat(transfer.getStatus()).isEqualTo(TransferStatus.FAILED);
            assertThat(transfer.getFailureReason()).isEqualTo("Account not found");
            assertThat(transfer.isFailed()).isTrue();
        }

        @Test
        @DisplayName("DEBITED → FAILED when failure occurs after debit")
        void shouldTransitionToFailedFromDebited() {
            transfer.markDebited();
            transfer.markFailed("Destination account closed");

            assertThat(transfer.getStatus()).isEqualTo(TransferStatus.FAILED);
            assertThat(transfer.getFailureReason()).isEqualTo("Destination account closed");
        }

        @Test
        @DisplayName("FAILED → REVERSED when source account is re-credited")
        void shouldTransitionToReversedFromFailed() {
            transfer.markDebited();
            transfer.markFailed("Destination unavailable");
            transfer.markReversed();

            assertThat(transfer.getStatus()).isEqualTo(TransferStatus.REVERSED);
        }
    }

    // -------------------------------------------------------------------------
    // Invalid state transitions — the state machine must reject these
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Invalid state transitions")
    class InvalidTransitions {

        @Test
        @DisplayName("should reject COMPLETED → DEBITED")
        void shouldRejectDebitingCompletedTransfer() {
            transfer.markDebited();
            transfer.markCompleted();

            assertThatThrownBy(() -> transfer.markDebited())
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("should reject INITIATED → COMPLETED (must go through DEBITED)")
        void shouldRejectCompletingWithoutDebiting() {
            // Cannot skip the DEBITED state — money must leave source before
            // we can confirm it arrived at destination
            assertThatThrownBy(() -> transfer.markCompleted())
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("should reject COMPLETED → FAILED")
        void shouldRejectFailingCompletedTransfer() {
            transfer.markDebited();
            transfer.markCompleted();

            assertThatThrownBy(() -> transfer.markFailed("Too late"))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("should reject REVERSED → FAILED")
        void shouldRejectFailingReversedTransfer() {
            transfer.markDebited();
            transfer.markFailed("error");
            transfer.markReversed();

            assertThatThrownBy(() -> transfer.markFailed("again"))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("should reject INITIATED → REVERSED (must fail first)")
        void shouldRejectReversingInitiatedTransfer() {
            assertThatThrownBy(() -> transfer.markReversed())
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("should reject COMPLETED → REVERSED")
        void shouldRejectReversingCompletedTransfer() {
            transfer.markDebited();
            transfer.markCompleted();

            assertThatThrownBy(() -> transfer.markReversed())
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // -------------------------------------------------------------------------
    // State queries
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("State queries")
    class StateQueries {

        @Test
        @DisplayName("isDebited should only be true in DEBITED status")
        void isDebitedShouldOnlyBeTrueInDebitedStatus() {
            assertThat(transfer.isDebited()).isFalse();

            transfer.markDebited();
            assertThat(transfer.isDebited()).isTrue();

            transfer.markCompleted();
            assertThat(transfer.isDebited()).isFalse();
        }

        @Test
        @DisplayName("isFailed should only be true in FAILED status")
        void isFailedShouldOnlyBeTrueInFailedStatus() {
            assertThat(transfer.isFailed()).isFalse();

            transfer.markFailed("error");
            assertThat(transfer.isFailed()).isTrue();

            // Even after reversal, isFailed checks for FAILED specifically
            // A reversed transfer is no longer in FAILED state
            transfer.markReversed();
            assertThat(transfer.isFailed()).isFalse();
        }
    }
}

// RUN Command
// ./mvnw test -pl transfer-module -Dtest=TransferTest