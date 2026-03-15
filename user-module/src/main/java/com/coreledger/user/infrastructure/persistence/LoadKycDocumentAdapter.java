// user-module/src/main/java/com/coreledger/user/infrastructure/persistence/kya.java
package com.coreledger.user.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.coreledger.shared.storage.DocumentStorageService;
import com.coreledger.user.application.port.out.LoadKycDocumentPort;
import com.coreledger.user.domain.model.KycDocumentBinary;
import com.coreledger.user.domain.model.KycDocumentId;

@Component
public class LoadKycDocumentAdapter implements LoadKycDocumentPort {

    private final KycDocumentJpaRepository kycDocumentJpaRepository;
    private final DocumentStorageService storageService;

    public LoadKycDocumentAdapter(KycDocumentJpaRepository repo,
            DocumentStorageService storageService) {
        this.kycDocumentJpaRepository = repo;
        this.storageService = storageService;
    }

    @Override
    public Optional<KycDocumentBinary> fetchDocument(KycDocumentId documentId) {
        return kycDocumentJpaRepository.findById(documentId.getValue())
                .map(entity -> {
                    byte[] content = storageService.load(entity.getStoragePath());
                    return new KycDocumentBinary(
                            KycDocumentId.of(entity.getId()),
                            deriveFilename(entity),
                            content,
                            deriveContentType(entity.getStoragePath()));
                });
    }

    private String deriveFilename(KycDocumentJpaEntity entity) {
        return entity.getType().name() + "-" + entity.getId();
    }

    private String deriveContentType(String storagePath) {
        if (storagePath.endsWith(".pdf"))
            return "application/pdf";
        if (storagePath.endsWith(".jpg") || storagePath.endsWith(".jpeg"))
            return "image/jpeg";
        if (storagePath.endsWith(".png"))
            return "image/png";
        return "application/octet-stream";
    }
}
