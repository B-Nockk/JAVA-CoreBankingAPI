// user-module/src/main/java/com/coreledger/user/domain/model/KycStatus.java

package com.coreledger.user.domain.model;

/**
 * Lifecycle status of a KYC document.
 */
public enum KycDocumentStatus {
    SUBMITTED, // uploaded but not reviewed
    VERIFIED, // accepted by compliance
    REJECTED // rejected, with reason
}
