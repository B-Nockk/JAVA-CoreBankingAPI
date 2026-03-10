// transfer-module/src/main/java/com/coreledger/transfer/infrastructure/web/TransferController.java
package com.coreledger.transfer.infrastructure.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coreledger.transfer.application.port.in.GetTransferUseCase;
import com.coreledger.transfer.application.port.in.InitiateTransferUseCase;
import com.coreledger.transfer.infrastructure.web.request.InitiateTransferRequest;
import com.coreledger.transfer.infrastructure.web.response.TransferResponse;

import jakarta.validation.Valid;

/**
 * REST adapter — inbound web layer for the transfer bounded context.
 *
 * Depends on use case interfaces, never TransferService directly.
 * Same discipline as AccountController.
 */
@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final InitiateTransferUseCase initiateTransferUseCase;
    private final GetTransferUseCase getTransferUseCase;

    public TransferController(
            InitiateTransferUseCase initiateTransferUseCase,
            GetTransferUseCase getTransferUseCase) {
        this.initiateTransferUseCase = initiateTransferUseCase;
        this.getTransferUseCase = getTransferUseCase;
    }

    // POST /api/v1/transfers
    @PostMapping
    public ResponseEntity<TransferResponse> initiateTransfer(
            @Valid @RequestBody InitiateTransferRequest request) {
        InitiateTransferUseCase.Command command = new InitiateTransferUseCase.Command(
                request.sourceAccountNumber(),
                request.destinationAccountNumber(),
                request.amount(),
                "SYSTEM");

        InitiateTransferUseCase.TransferResult result = initiateTransferUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(TransferResponse.from(result));
    }

    // GET /api/v1/transfers/{transferId}
    @GetMapping("/{transferId}")
    public ResponseEntity<TransferResponse> getTransfer(
            @PathVariable String transferId) {
        GetTransferUseCase.TransferResult result = getTransferUseCase.getById(transferId);
        return ResponseEntity.ok(TransferResponse.from(result));
    }
}
