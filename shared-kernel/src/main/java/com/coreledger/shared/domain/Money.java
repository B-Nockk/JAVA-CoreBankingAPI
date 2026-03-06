// shared-kernel/src/main/java/com/coreledger/shared/Money.java
package com.coreledger.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object representing a monetary amount with its currency.
 *
 * Design rules enforced here:
 * - Immutable: every operation returns a new Money instance
 * - Currency-safe: arithmetic across different currencies throws immediately
 * - Precision: always scaled to 2 decimal places using HALF_EVEN (banker's
 * rounding)
 * - No nulls: amount and currency are required at construction
 *
 * HALF_EVEN (banker's rounding) rounds 0.5 to the nearest even digit.
 * Example: 2.5 → 2, 3.5 → 4. This minimises cumulative rounding bias
 * over large transaction volumes — standard in financial systems.
 */
public final class Money {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    private final BigDecimal amount;
    private final Currency currency;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    private Money(BigDecimal amount, Currency currency) {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        this.amount = amount.setScale(SCALE, ROUNDING);
        this.currency = currency;
    }

    /**
     * Primary factory method.
     * Use this throughout the codebase — never call new Money() directly.
     */
    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    /**
     * Convenience factory for string input (e.g. from API requests).
     * "100.50" → Money(100.50, currency)
     */
    public static Money of(String amount, Currency currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    /**
     * Zero value for a given currency.
     * Useful as a starting point when deriving balance from transactions.
     */
    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    // -------------------------------------------------------------------------
    // Arithmetic — all return new instances, nothing mutates
    // -------------------------------------------------------------------------

    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public Money multiply(BigDecimal factor) {
        return new Money(this.amount.multiply(factor), this.currency);
    }

    // -------------------------------------------------------------------------
    // Comparison
    // -------------------------------------------------------------------------

    public boolean isGreaterThan(Money other) {
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isGreaterThanOrEqualTo(Money other) {
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount) >= 0;
    }

    public boolean isLessThan(Money other) {
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }

    public boolean isNegative() {
        return this.amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    // -------------------------------------------------------------------------
    // Equality
    // -------------------------------------------------------------------------

    /**
     * Uses compareTo, not equals, on BigDecimal.
     * This means Money(1.50, NGN) == Money(1.5, NGN).
     * BigDecimal.equals() would treat them as different due to scale — wrong for
     * Money.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Money other))
            return false;
        return this.amount.compareTo(other.amount) == 0
                && this.currency == other.currency;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }

    @Override
    public String toString() {
        return currency.name() + " " + amount.toPlainString();
    }

    // -------------------------------------------------------------------------
    // Internal guards
    // -------------------------------------------------------------------------

    private void assertSameCurrency(Money other) {
        if (this.currency != other.currency) {
            throw new IllegalArgumentException(
                    String.format("Currency mismatch: cannot operate on %s and %s",
                            this.currency, other.currency));
        }
    }
}