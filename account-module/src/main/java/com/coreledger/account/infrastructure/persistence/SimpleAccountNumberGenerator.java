// account-module/src/main/java/com/coreledger/account/infrastructure/persistence/SimpleAccountNumberGenerator.java
package com.coreledger.account.infrastructure.persistence;

import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

import com.coreledger.account.application.port.out.AccountNumberGeneratorPort;

/**
 * Simple account number generator for development and testing.
 *
 * Generates a random 10-digit numeric string.
 * This is intentionally NOT NUBAN-compliant — that is a future upgrade.
 *
 * NUBAN (Nigeria Uniform Bank Account Number) compliance requires:
 * - 3-digit bank code prefix (assigned by CBN)
 * - 6-digit serial number
 * - 1 check digit computed via the CBN algorithm:
 * multiply each of the first 9 digits by the weight [3,7,3,3,7,3,3,7,3]
 * sum the products, mod 10, subtract from 10, mod 10
 *
 * When NUBAN compliance is needed, create NubanAccountNumberGenerator
 * implementing the same port and swap the @Primary annotation.
 * Nothing else in the codebase changes — this is the port abstraction paying
 * off.
 *
 * Uniqueness note: random generation has collision risk at scale.
 * Production systems use a DB sequence or a dedicated number-range service.
 * For this project, random is sufficient and keeps the focus on architecture.
 */
@Component
public class SimpleAccountNumberGenerator implements AccountNumberGeneratorPort {

    @Override
    public String generate() {
        long number = ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L);
        return String.valueOf(number);
    }
}