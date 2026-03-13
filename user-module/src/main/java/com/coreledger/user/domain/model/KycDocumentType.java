// user-module/src/main/java/com/coreledger/user/domain/model/KycDocumentType.java
package com.coreledger.user.domain.model;

/**
 * Enum representing the types of KYC documents
 * that can be submitted by a user.
 */
public enum KycDocumentType {
    EMAIL,
    PHONE,
    ID_CARD,
    ADDRESS_PROOF,
    BIOMETRICS,
    CORPORATE_DOC
}
