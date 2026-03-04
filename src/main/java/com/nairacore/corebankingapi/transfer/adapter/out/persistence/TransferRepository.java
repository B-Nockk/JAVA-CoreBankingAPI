package com.nairacore.corebankingapi.transfer.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TransferRepository extends JpaRepository<TransferJpaEntity, Long> {

    // Finds the row with the highest version number for a specific transferId
    Optional<TransferJpaEntity> findTopByTransferIdOrderByVersionDesc(String transferId);
}