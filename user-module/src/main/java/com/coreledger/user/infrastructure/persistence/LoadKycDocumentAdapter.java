// user-module/src/main/java/com/coreledger/user/infrastructure/persistence/LoadKycDocumentAdapter.java
package com.coreledger.user.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.coreledger.shared.storage.DocumentStorageService;
import com.coreledger.user.application.port.out.LoadKycDocumentPort;
import com.coreledger.user.domain.model.KycDocument;
import com.coreledger.user.domain.model.KycDocumentBinary;
import com.coreledger.user.domain.model.KycDocumentId;
import com.coreledger.user.domain.model.KycDocumentStatus;
import com.coreledger.user.domain.model.KycProfileId;

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
    @Transactional(readOnly = true)
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

    @Override
    @Transactional(readOnly = true)
    public Optional<KycDocument> loadDocument(KycDocumentId documentId) {
        return kycDocumentJpaRepository.findById(documentId.getValue())
                .map(this::toDomainDocument);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KycDocument> loadDocumentsForProfile(KycProfileId profileId) {
        return kycDocumentJpaRepository.findByKycProfileId(profileId.getValue())
                .stream()
                .map(this::toDomainDocument)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean documentExists(KycDocumentId documentId) {
        return kycDocumentJpaRepository.existsById(documentId.getValue());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getStoragePath(KycDocumentId documentId) {
        return kycDocumentJpaRepository.findById(documentId.getValue())
                .map(KycDocumentJpaEntity::getStoragePath);
    }

    // ============================
    // Private helpers
    // ============================

    private KycDocument toDomainDocument(KycDocumentJpaEntity entity) {
        KycDocument doc = new KycDocument(
                KycDocumentId.of(entity.getId()),
                KycProfileId.of(entity.getKycProfileId()),
                entity.getType());

        if (entity.getStatus() == KycDocumentStatus.VERIFIED) {
            doc.markVerified();
        } else if (entity.getStatus() == KycDocumentStatus.REJECTED) {
            doc.markRejected(entity.getRejectionReason());
        }
        return doc;
    }

    private String deriveFilename(KycDocumentJpaEntity entity) {
        String base = entity.getType().name() + "-" + entity.getId();
        String extension = getFileExtension(entity.getStoragePath());
        return base + extension;
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