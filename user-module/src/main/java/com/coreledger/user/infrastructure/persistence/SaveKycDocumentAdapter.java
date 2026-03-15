// user-module/src/main/java/com/coreledger/user/infrastructure/persistence/SaveKycDocumentAdapter.java
package com.coreledger.user.infrastructure.persistence;

import org.springframework.stereotype.Component;

import com.coreledger.shared.storage.DocumentStorageService;
import com.coreledger.user.application.port.out.SaveKycDocumentPort;
import com.coreledger.user.domain.model.DocumentFileType;
import com.coreledger.user.domain.model.KycDocument;

@Component
public class SaveKycDocumentAdapter implements SaveKycDocumentPort {

    private final KycDocumentJpaRepository kycDocumentJpaRepository;
    private final DocumentStorageService storageService;

    public SaveKycDocumentAdapter(KycDocumentJpaRepository repo,
            DocumentStorageService storageService) {
        this.kycDocumentJpaRepository = repo;
        this.storageService = storageService;
    }

    @Override
    public KycDocument save(KycDocument kycDocument, byte[] content, String filename) {
        // 1. Validate file type
        DocumentFileType fileType = DocumentFileType.fromFilename(filename);

        // 2. Save binary to storage backend
        String storagePath = storageService.save(content, filename, fileType.mimeType());

        // 3. Persist metadata in SQL
        KycDocumentJpaEntity entity = new KycDocumentJpaEntity();
        entity.setId(kycDocument.getId().getValue());
        entity.setKycProfileId(kycDocument.getProfileId().getValue());
        entity.setType(kycDocument.getType());
        entity.setStatus(kycDocument.getStatus());
        entity.setRejectionReason(kycDocument.getRejectionReason());
        entity.setStoragePath(storagePath);

        kycDocumentJpaRepository.save(entity);

        // 4. Return updated domain object
        return kycDocument;
    }
}
