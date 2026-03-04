// src/main/java/com/nairacore/corebankingapi/transfer/adapter/out/persistence/TransferRepositoryPort.java
package com.nairacore.corebankingapi.transfer.adapter.out.persistence;

import com.nairacore.corebankingapi.transfer.domain.Transfer;

public interface TransferRepositoryPort {
    // The implementation of this port MUST be annotated with:
    // @Transactional(propagation = Propagation.REQUIRES_NEW)
    void append(Transfer transfer);
}
