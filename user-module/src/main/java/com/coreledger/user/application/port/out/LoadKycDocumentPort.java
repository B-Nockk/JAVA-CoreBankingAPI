// user-module/src/main/java/com/coreledger/user/application/port/out/LoadKycDocumentPort.java
package com.coreledger.user.application.port.out;

import java.util.Optional;

import com.coreledger.user.domain.model.KycDocumentBinary;
import com.coreledger.user.domain.model.KycDocumentId;

public interface LoadKycDocumentPort {
    Optional<KycDocumentBinary> fetchDocument(KycDocumentId documentId);
}
