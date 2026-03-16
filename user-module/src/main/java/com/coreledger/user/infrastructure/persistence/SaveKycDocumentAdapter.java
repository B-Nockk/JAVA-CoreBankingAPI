// user-module/src/main/java/com/coreledger/user/infrastructure/persistence/SaveKycDocumentAdapter.java
package com.coreledger.user.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.coreledger.shared.storage.DocumentStorageService;
import com.coreledger.user.application.port.out.SaveKycDocumentPort;
import com.coreledger.user.domain.model.DocumentFileType;
import com.coreledger.user.domain.model.KycDocument;
import com.coreledger.user.domain.model.KycDocumentBinary;
import com.coreledger.user.domain.model.KycDocumentId;

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
    @Transactional
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

    @Override
    @Transactional
    public KycDocument updateMetadata(KycDocument kycDocument) {
        KycDocumentJpaEntity entity = kycDocumentJpaRepository.findById(kycDocument.getId().getValue())
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + kycDocument.getId()));

        // Update only metadata fields
        entity.setStatus(kycDocument.getStatus());
        entity.setRejectionReason(kycDocument.getRejectionReason());
        // Note: type, profileId, and storagePath should not change

        kycDocumentJpaRepository.save(entity);
        return kycDocument;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<KycDocumentBinary> fetchBinary(KycDocumentId documentId) {
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
        return entity.getType().name() + "-" + entity.getId() + getFileExtension(entity.getStoragePath());
    }

    private String getFileExtension(String storagePath) {
        if (storagePath.contains(".")) {
            return storagePath.substring(storagePath.lastIndexOf("."));
        }
        return "";
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