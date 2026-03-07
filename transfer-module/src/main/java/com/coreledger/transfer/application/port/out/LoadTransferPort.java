// transfer-module/src/main/java/com/coreledger/transfer/application/port/out/LoadTransferPort.java
package com.coreledger.transfer.application.port.out;

import java.util.Optional;

import com.coreledger.transfer.domain.model.Transfer;
import com.coreledger.transfer.domain.model.TransferId;

/**
 * Outbound port — defines what the application needs from persistence
 * to load a Transfer.
 */
public interface LoadTransferPort {

    Optional<Transfer> findById(TransferId id);
}