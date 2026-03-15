// user-module/src/main/java/com/coreledger/user/application/port/in/LoadKycDocumentUseCase.java
package com.coreledger.user.application.port.in;

import java.util.Optional;
import com.coreledger.user.domain.model.KycDocumentBinary;
import com.coreledger.user.domain.model.KycDocumentId;

public interface LoadKycDocumentUseCase {
    Optional<KycDocumentBinary> loadDocument(KycDocumentId documentId);
}
