// user-module/src/main/java/com/coreledger/user/infrastructure/persistence/UserKycPersistenceAdapter.java
package com.coreledger.user.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.coreledger.user.application.port.out.LoadUserKycPort;
import com.coreledger.user.application.port.out.SaveUserKycPort;
import com.coreledger.user.domain.model.KycDocument;
import com.coreledger.user.domain.model.KycDocumentId;
import com.coreledger.user.domain.model.KycDocumentStatus;
import com.coreledger.user.domain.model.KycProfile;
import com.coreledger.user.domain.model.KycProfileId;
import com.coreledger.user.domain.model.UserId;

@Component
public class UserKycPersistenceAdapter implements LoadUserKycPort, SaveUserKycPort {

    private final KycJpaRepository kycJpaRepository;
    private final KycDocumentJpaRepository kycDocumentJpaRepository;

    public UserKycPersistenceAdapter(KycJpaRepository kycJpaRepository,
            KycDocumentJpaRepository kycDocumentJpaRepository) {
        this.kycJpaRepository = kycJpaRepository;
        this.kycDocumentJpaRepository = kycDocumentJpaRepository;
    }

    @Override
    public Optional<KycProfile> findByUserId(UserId userId) {
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

    @Override
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
        KycDocument doc = new KycDocument(KycDocumentId.of(entity.getId()), (KycProfileId.of(entity.getKycProfileId())),
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
        // TODO:: storagePath would be set elsewhere when file is uploaded
        return entity;
    }
}
