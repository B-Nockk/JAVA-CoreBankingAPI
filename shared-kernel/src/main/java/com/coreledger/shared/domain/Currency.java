// shared-kernel/src/main/java/com/coreledger/shared/domain/Currency.java
package com.coreledger.shared.domain;

/**
 * Supported currencies in CoreLedger.
 *
 * Using an enum (not java.util.Currency) because:
 * - We control exactly which currencies the platform supports
 * - Compile-time safety: no invalid currency strings at runtime
 * - Easy to extend: add a new currency here and the type system
 * will surface every place that needs updating
 *
 * Each entry carries the ISO 4217 numeric scale (decimal places).
 * Most currencies use 2. Some (e.g. JPY) use 0. Crypto would use more.
 * This allows Money to be currency-aware about precision if needed later.
 */
public enum Currency {

    // African
    NGN(2, "Nigerian Naira"),
    GHS(2, "Ghanaian Cedi"),
    KES(2, "Kenyan Shilling"),
    ZAR(2, "South African Rand"),

    // Major international
    USD(2, "US Dollar"),
    EUR(2, "Euro"),
    GBP(2, "British Pound"),

    // Zero-decimal example (useful to have early for correctness testing)
    JPY(0, "Japanese Yen");

    private final int defaultScale;
    private final String displayName;

    Currency(int defaultScale, String displayName) {
        this.defaultScale = defaultScale;
        this.displayName = displayName;
    }

    public int getDefaultScale() {
        return defaultScale;
    }

    public String getDisplayName() {
        return displayName;
    }
}