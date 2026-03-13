// user-module/src/main/java/com/coreledger/user/domain/model/KycTier.java
package com.coreledger.user.domain.model;

/**
 * Enum representing the regulatory KYC tiers.
 * Each tier corresponds to a level of verification
 * and determines transaction limits and account restrictions.
 */
public enum KycTier {
    TIER_1, // Minimal info: phone/email only
    TIER_2, // Medium verification: ID card verified
    TIER_3, // Full verification: ID + address proof + biometrics
    TIER_4 // Optional: corporate or advanced tier
}
