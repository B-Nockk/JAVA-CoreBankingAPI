// account-module/src/main/java/com/coreledger/account/application/port/out/AccountNumberGeneratorPort.java
package com.coreledger.account.application.port.out;

/**
 * Outbound port — defines the contract for generating a unique account number.
 *
 * Why is this a port and not just a utility class?
 * Account number generation is a business rule with real-world constraints:
 * - It must be unique across all accounts
 * - The format may be regulated (e.g. NUBAN format in Nigeria)
 * - The generation strategy may need to change (sequential, random,
 * bank-code-prefixed)
 *
 * By making it a port, the domain stays decoupled from the generation
 * mechanism.
 * In tests you inject a simple stub. In production you inject the
 * NUBAN-compliant
 * implementation. The service never knows the difference.
 *
 * NUBAN (Nigeria Uniform Bank Account Number) context for reference:
 * 10-digit format: [3-digit bank code] + [6-digit serial] + [1 check digit]
 * The check digit is computed via a specific CBN algorithm.
 * We abstract that behind this port so the domain stays clean.
 */
public interface AccountNumberGeneratorPort {

    String generate();
}