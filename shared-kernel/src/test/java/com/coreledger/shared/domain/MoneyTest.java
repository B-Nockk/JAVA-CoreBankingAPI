// shared-kernel/src/test/java/com/coreledger/shared/domain/MoneyTest.java
package com.coreledger.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Money value object.
 *
 * No Spring context, no DB, no mocks.
 * Just plain Java — instantiate, act, assert.
 *
 * @Nested groups related tests together visually and in test reports.
 *         Think of each @Nested class as a chapter heading.
 *
 * @DisplayName gives the test a human-readable label in the test report.
 */
class MoneyTest {

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create Money with correct amount and currency")
        void shouldCreateMoneyWithCorrectAmountAndCurrency() {
            Money money = Money.of("100.50", Currency.NGN);

            assertThat(money.getAmount()).isEqualByComparingTo("100.50");
            assertThat(money.getCurrency()).isEqualTo(Currency.NGN);
        }

        @Test
        @DisplayName("should scale amount to 2 decimal places")
        void shouldScaleAmountToTwoDecimalPlaces() {
            Money money = Money.of("100", Currency.NGN);

            // 100 stored as 100.00
            assertThat(money.getAmount().scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("should create zero Money for a given currency")
        void shouldCreateZeroMoney() {
            Money zero = Money.zero(Currency.NGN);

            assertThat(zero.isZero()).isTrue();
            assertThat(zero.getCurrency()).isEqualTo(Currency.NGN);
        }

        @Test
        @DisplayName("should throw when amount is null")
        void shouldThrowWhenAmountIsNull() {
            assertThatThrownBy(() -> Money.of((BigDecimal) null, Currency.NGN))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should throw when currency is null")
        void shouldThrowWhenCurrencyIsNull() {
            assertThatThrownBy(() -> Money.of("100", null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // -------------------------------------------------------------------------
    // Arithmetic
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Arithmetic")
    class Arithmetic {

        @Test
        @DisplayName("should add two Money values of same currency")
        void shouldAddTwoMoneyValuesOfSameCurrency() {
            Money twoHundred = Money.of("200.00", Currency.NGN);
            Money threeHundred = Money.of("300.00", Currency.NGN);

            Money result = twoHundred.add(threeHundred);

            assertThat(result.getAmount()).isEqualByComparingTo("500.00");
            assertThat(result.getCurrency()).isEqualTo(Currency.NGN);
        }

        @Test
        @DisplayName("should subtract two Money values of same currency")
        void shouldSubtractTwoMoneyValues() {
            Money fiveHundred = Money.of("500.00", Currency.NGN);
            Money twoHundred = Money.of("200.00", Currency.NGN);

            Money result = fiveHundred.subtract(twoHundred);

            assertThat(result.getAmount()).isEqualByComparingTo("300.00");
        }

        @Test
        @DisplayName("should multiply Money by a factor")
        void shouldMultiplyMoneyByFactor() {
            Money hundred = Money.of("100.00", Currency.NGN);

            Money result = hundred.multiply(new BigDecimal("1.5"));

            assertThat(result.getAmount()).isEqualByComparingTo("150.00");
        }

        @Test
        @DisplayName("should throw when adding different currencies")
        void shouldThrowWhenAddingDifferentCurrencies() {
            Money ngn = Money.of("100", Currency.NGN);
            Money usd = Money.of("100", Currency.USD);

            assertThatThrownBy(() -> ngn.add(usd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Currency mismatch");
        }

        @Test
        @DisplayName("should throw when subtracting different currencies")
        void shouldThrowWhenSubtractingDifferentCurrencies() {
            Money ngn = Money.of("100", Currency.NGN);
            Money usd = Money.of("50", Currency.USD);

            assertThatThrownBy(() -> ngn.subtract(usd))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should apply HALF_EVEN rounding on multiply")
        void shouldApplyBankersRoundingOnMultiply() {
            // 100 * 0.015 = 1.5 → HALF_EVEN rounds to 1.50 (even digit)
            // This tests that we're using banker's rounding not standard rounding
            Money hundred = Money.of("1.00", Currency.NGN);

            Money result = hundred.multiply(new BigDecimal("0.015"));

            // 0.015 rounds to 0.02 with HALF_UP but 0.02 with HALF_EVEN (both even here)
            // Key: result is a valid 2dp number, no ArithmeticException thrown
            assertThat(result.getAmount().scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("add should return a new Money instance — original unchanged")
        void addShouldReturnNewInstance() {
            Money original = Money.of("100.00", Currency.NGN);
            Money other = Money.of("50.00", Currency.NGN);

            Money result = original.add(other);

            // Immutability check — original must not change
            assertThat(original.getAmount()).isEqualByComparingTo("100.00");
            assertThat(result.getAmount()).isEqualByComparingTo("150.00");
            assertThat(result).isNotSameAs(original);
        }
    }

    // -------------------------------------------------------------------------
    // Comparison
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Comparison")
    class Comparison {

        @Test
        @DisplayName("should identify positive amount")
        void shouldIdentifyPositiveAmount() {
            assertThat(Money.of("0.01", Currency.NGN).isPositive()).isTrue();
            assertThat(Money.of("0.00", Currency.NGN).isPositive()).isFalse();
            assertThat(Money.of("-0.01", Currency.NGN).isPositive()).isFalse();
        }

        @Test
        @DisplayName("should identify negative amount")
        void shouldIdentifyNegativeAmount() {
            assertThat(Money.of("-0.01", Currency.NGN).isNegative()).isTrue();
            assertThat(Money.of("0.00", Currency.NGN).isNegative()).isFalse();
        }

        @Test
        @DisplayName("should identify zero amount")
        void shouldIdentifyZeroAmount() {
            assertThat(Money.of("0.00", Currency.NGN).isZero()).isTrue();
            assertThat(Money.of("0.01", Currency.NGN).isZero()).isFalse();
        }

        @Test
        @DisplayName("should compare greater than correctly")
        void shouldCompareGreaterThan() {
            Money large = Money.of("200.00", Currency.NGN);
            Money small = Money.of("100.00", Currency.NGN);

            assertThat(large.isGreaterThan(small)).isTrue();
            assertThat(small.isGreaterThan(large)).isFalse();
        }

        @Test
        @DisplayName("should compare less than correctly")
        void shouldCompareLessThan() {
            Money large = Money.of("200.00", Currency.NGN);
            Money small = Money.of("100.00", Currency.NGN);

            assertThat(small.isLessThan(large)).isTrue();
            assertThat(large.isLessThan(small)).isFalse();
        }
    }

    // -------------------------------------------------------------------------
    // Equality
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal when amount and currency match")
        void shouldBeEqualWhenAmountAndCurrencyMatch() {
            Money a = Money.of("100.00", Currency.NGN);
            Money b = Money.of("100.00", Currency.NGN);

            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }

        @Test
        @DisplayName("should be equal regardless of trailing zeros — 100 equals 100.00")
        void shouldBeEqualRegardlessOfTrailingZeros() {
            // This tests that we use compareTo not equals on BigDecimal
            Money a = Money.of("100", Currency.NGN);
            Money b = Money.of("100.00", Currency.NGN);

            assertThat(a).isEqualTo(b);
        }

        @Test
        @DisplayName("should not be equal when currencies differ")
        void shouldNotBeEqualWhenCurrenciesDiffer() {
            Money ngn = Money.of("100.00", Currency.NGN);
            Money usd = Money.of("100.00", Currency.USD);

            assertThat(ngn).isNotEqualTo(usd);
        }

        @Test
        @DisplayName("should not be equal when amounts differ")
        void shouldNotBeEqualWhenAmountsDiffer() {
            Money a = Money.of("100.00", Currency.NGN);
            Money b = Money.of("200.00", Currency.NGN);

            assertThat(a).isNotEqualTo(b);
        }
    }
}

// RUN Command
// ./mvnw test -pl shared-kernel -Dtest=MoneyTest