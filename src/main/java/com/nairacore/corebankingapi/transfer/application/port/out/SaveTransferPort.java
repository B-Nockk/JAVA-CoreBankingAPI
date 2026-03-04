// src/main/java/com/nairacore/corebankingapi/transfer/application/port/out/SaveTransferPort.java
package com.nairacore.corebankingapi.transfer.application.port.out;

import com.nairacore.corebankingapi.transfer.domain.Transfer;

public interface SaveTransferPort {
    void save(Transfer transfer);
}