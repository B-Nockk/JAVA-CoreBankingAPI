// user-module/src/main/java/com/coreledger/user/application/port/out/SaveKycDocumentPort.java
package com.coreledger.user.application.port.out;

import com.coreledger.user.domain.model.KycDocument;

public interface SaveKycDocumentPort {
    KycDocument save(KycDocument kycDocument, byte[] content, String filename);
}
