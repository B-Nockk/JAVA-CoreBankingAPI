// user-module/src/main/java/com/coreledger/user/infrastructure/persistence/KycJpaEntity.java
package com.coreledger.user.infrastructure.persistence;

import java.util.UUID;

import com.coreledger.user.domain.model.KycTier;
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
@Table(name = "kyc_profile")
@Getter
@Setter
@NoArgsConstructor
public class KycJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "userId", nullable = false)
    private UUID userId; // FK to User

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false)
    private KycTier tier;
}
