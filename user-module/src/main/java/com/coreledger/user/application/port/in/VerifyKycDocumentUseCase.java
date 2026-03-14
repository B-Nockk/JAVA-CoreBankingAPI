// user-module/src/main/java/com/coreledger/user/application/port/in/VerifyKycDocumentUseCase.java
package com.coreledger.user.application.port.in;

import java.util.UUID;

public interface VerifyKycDocumentUseCase {
    void verifyDocument(UUID kycDocumentId);
}
