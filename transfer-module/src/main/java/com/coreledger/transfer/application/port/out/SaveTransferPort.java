// transfer-module/src/main/java/com/coreledger/transfer/application/port/out/SaveTransferPort.java
package com.coreledger.transfer.application.port.out;

import com.coreledger.transfer.domain.model.Transfer;

/**
 * Outbound port — defines what the application needs from persistence
 * to save Transfer state.
 *
 * Used for both initial creation and status updates (DEBITED, COMPLETED,
 * FAILED, REVERSED) as the transfer moves through its lifecycle.
 */
public interface SaveTransferPort {

    Transfer save(Transfer transfer);
}