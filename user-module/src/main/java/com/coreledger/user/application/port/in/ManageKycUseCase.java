// user-module/src/main/java/com/coreledger/user/application/port/in/ManageKycUseCase.java
package com.coreledger.user.application.port.in;

/**
 * Combined interface for all KYC operations.
 * Makes it easier to inject a single dependency in controllers/services
 * that need full KYC functionality.
 */
public interface ManageKycUseCase extends
        GetUserKycUseCase,
        AddKycDocumentUseCase,
        VerifyKycDocumentUseCase,
        RejectKycDocumentUseCase,
        LoadKycDocumentUseCase,
        UpdateUserKycUseCase,
        DeleteKycDocumentUseCase,
        DeleteKycProfileUseCase {

    // No additional methods needed - just combining existing ones
}