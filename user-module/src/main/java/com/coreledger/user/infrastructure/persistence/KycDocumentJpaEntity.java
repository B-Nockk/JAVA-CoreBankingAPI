package com.coreledger.user.infrastructure.persistence;

import java.util.UUID;

import com.coreledger.user.domain.model.KycDocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "kyc_documents")
@Getter
@Setter
@NoArgsConstructor
public class KycDocumentJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "kycProfileId", nullable = false)
    private UUID kycProfileId; // FK to KycProfile

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private KycDocumentType type;

    @Column(name = "verified", nullable = false)
    private boolean verified;

    @Column(name = "storagePath", nullable = false)
    private String storagePath; // reference to file storage (S3, DB blob, etc.)
}
