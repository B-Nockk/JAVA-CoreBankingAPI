// shared-kernel/src/main/java/com/coreledger/shared/domain/Money.java
package com.coreledger.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Value Object representing a monetary amount with its currency.
 *
 * Jackson notes:
 * - @JsonCreator on the private constructor lets Jackson deserialize directly.
 * - @JsonProperty on getAmount/getCurrency controls what gets serialized.
 * - isZero/isNegative/isPositive are NOT annotated — Jackson ignores them
 * because they have no corresponding @JsonProperty and the constructor
 * doesn't reference them. This keeps the wire format clean:
 * {"amount": 3000.00, "currency": "NGN"}
 */
public final class Money {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    private final BigDecimal amount;
    private final Currency currency;

    // Used by Jackson for deserialization — private keeps the domain API clean.
    @JsonCreator
    private Money(
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("currency") Currency currency) {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        this.amount = amount.setScale(SCALE, ROUNDING);
        this.currency = currency;
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money of(String amount, Currency currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    // ── Arithmetic ────────────────────────────────────────────────────────────

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

    // ── Comparison ────────────────────────────────────────────────────────────

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

    @JsonIgnore
    public boolean isNegative() {
        return this.amount.compareTo(BigDecimal.ZERO) < 0;
    }

    @JsonIgnore
    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    @JsonIgnore
    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    // ── Accessors — @JsonProperty marks these for serialization ──────────────

    @JsonProperty("amount")
    public BigDecimal getAmount() {
        return amount;
    }

    @JsonProperty("currency")
    public Currency getCurrency() {
        return currency;
    }

    // ── Equality ──────────────────────────────────────────────────────────────

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

    private void assertSameCurrency(Money other) {
        if (this.currency != other.currency) {
            throw new IllegalArgumentException(
                    "Currency mismatch: cannot operate on %s and %s"
                            .formatted(this.currency, other.currency));
        }
    }
}