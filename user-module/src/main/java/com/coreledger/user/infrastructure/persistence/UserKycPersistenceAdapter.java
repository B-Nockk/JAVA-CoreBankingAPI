// user-module/src/main/java/com/coreledger/user/infrastructure/persistence/UserKycPersistenceAdapter.java
package com.coreledger.user.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.coreledger.shared.storage.DocumentStorageService;
import com.coreledger.user.application.port.out.DeleteKycPort;
import com.coreledger.user.application.port.out.LoadUserKycPort;
import com.coreledger.user.application.port.out.SaveUserKycPort;
import com.coreledger.user.domain.model.KycDocument;
import com.coreledger.user.domain.model.KycDocumentId;
import com.coreledger.user.domain.model.KycDocumentStatus;
import com.coreledger.user.domain.model.KycProfile;
import com.coreledger.user.domain.model.KycProfileId;
import com.coreledger.user.domain.model.UserId;

@Component
public class UserKycPersistenceAdapter implements LoadUserKycPort, SaveUserKycPort, DeleteKycPort {

    private final KycJpaRepository kycJpaRepository;
    private final KycDocumentJpaRepository kycDocumentJpaRepository;
    private final DocumentStorageService storageService; // Added for binary deletion

    public UserKycPersistenceAdapter(
            KycJpaRepository kycJpaRepository,
            KycDocumentJpaRepository kycDocumentJpaRepository,
            DocumentStorageService storageService) {
        this.kycJpaRepository = kycJpaRepository;
        this.kycDocumentJpaRepository = kycDocumentJpaRepository;
        this.storageService = storageService;
    }

    // ============================
    // LoadUserKycPort methods
    // ============================

    @Override
    @Transactional(readOnly = true)
    public Optional<KycProfile> loadKycProfile(UserId userId) {
        return kycJpaRepository.findByUserId(userId.getValue())
                .map(profileEntity -> {
                    List<KycDocument> docs = kycDocumentJpaRepository.findByKycProfileId(profileEntity.getId())
                            .stream()
                            .map(this::toDomainDocument)
                            .toList();
                    return toDomainProfile(profileEntity, docs);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<KycProfile> loadKycProfile(KycProfileId profileId) {
        return kycJpaRepository.findById(profileId.getValue())
                .map(profileEntity -> {
                    List<KycDocument> docs = kycDocumentJpaRepository.findByKycProfileId(profileEntity.getId())
                            .stream()
                            .map(this::toDomainDocument)
                            .toList();
                    return toDomainProfile(profileEntity, docs);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasKycProfile(UserId userId) {
        return kycJpaRepository.findByUserId(userId.getValue()).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<KycProfile> findAll(Pageable pageable) {
        return kycJpaRepository.findAll(pageable)
                .map(profileEntity -> {
                    List<KycDocument> docs = kycDocumentJpaRepository.findByKycProfileId(profileEntity.getId())
                            .stream()
                            .map(this::toDomainDocument)
                            .toList();
                    return toDomainProfile(profileEntity, docs);
                });
    }

    // ============================
    // SaveUserKycPort methods
    // ============================

    @Override
    @Transactional
    public KycProfile save(KycProfile profile) {
        KycJpaEntity profileEntity = toJpaEntity(profile);
        KycJpaEntity savedProfile = kycJpaRepository.save(profileEntity);

        // Save documents
        List<KycDocumentJpaEntity> docEntities = profile.getDocuments().stream()
                .map(d -> toJpaEntity(d, savedProfile.getId()))
                .toList();
        kycDocumentJpaRepository.saveAll(docEntities);

        return toDomainProfile(savedProfile, profile.getDocuments());
    }

    @Override
    @Transactional
    public KycProfile update(KycProfile profile) {
        // Check if profile exists
        KycJpaEntity existingProfile = kycJpaRepository.findById(profile.getId().getValue())
                .orElseThrow(() -> new RuntimeException("KycProfile not found with id: " + profile.getId()));

        // Update tier only (other fields shouldn't change)
        existingProfile.setTier(profile.getTier());

        KycJpaEntity savedProfile = kycJpaRepository.save(existingProfile);

        // Documents are handled separately via SaveKycDocumentPort
        return toDomainProfile(savedProfile, profile.getDocuments());
    }

    // ============================
    // DeleteKycPort methods
    // ============================

    @Override
    @Transactional
    public void deleteProfile(KycProfileId profileId) {
        // First delete all documents (including binaries)
        List<KycDocumentJpaEntity> documents = kycDocumentJpaRepository.findByKycProfileId(profileId.getValue());

        // Delete binaries from storage
        documents.forEach(doc -> {
            try {
                storageService.delete(doc.getStoragePath());
            } catch (Exception e) {
                // Log but continue - cleanup best effort
                // In production, you might want to queue this for retry
            }
        });

        // Delete document metadata
        kycDocumentJpaRepository.deleteAll(documents);

        // Delete profile
        kycJpaRepository.deleteById(profileId.getValue());
    }

    @Override
    @Transactional
    public void deleteDocument(KycDocumentId documentId) {
        KycDocumentJpaEntity document = kycDocumentJpaRepository.findById(documentId.getValue())
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + documentId));

        // Delete binary from storage
        storageService.delete(document.getStoragePath());

        // Delete metadata
        kycDocumentJpaRepository.delete(document);
    }

    @Override
    @Transactional
    public void deleteDocuments(Iterable<KycDocumentId> documentIds) {
        documentIds.forEach(this::deleteDocument);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasDocuments(KycProfileId profileId) {
        return !kycDocumentJpaRepository.findByKycProfileId(profileId.getValue()).isEmpty();
    }

    // ============================
    // Mapping helpers
    // ============================

    private KycProfile toDomainProfile(KycJpaEntity entity, List<KycDocument> documents) {
        return KycProfile.reconstitute(
                KycProfileId.from(entity.getId()),
                UserId.of(entity.getUserId()),
                documents,
                entity.getTier());
    }

    private KycJpaEntity toJpaEntity(KycProfile profile) {
        KycJpaEntity entity = new KycJpaEntity();
        entity.setId(profile.getId().getValue());
        entity.setUserId(profile.getUserId().getValue());
        entity.setTier(profile.getTier());
        return entity;
    }

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

    private KycDocumentJpaEntity toJpaEntity(KycDocument doc, UUID profileId) {
        KycDocumentJpaEntity entity = new KycDocumentJpaEntity();
        entity.setId(doc.getId().getValue());
        entity.setKycProfileId(profileId);
        entity.setType(doc.getType());
        entity.setStatus(doc.getStatus());
        entity.setRejectionReason(doc.getRejectionReason());
        // storagePath is set by SaveKycDocumentAdapter during upload
        return entity;
    }
}