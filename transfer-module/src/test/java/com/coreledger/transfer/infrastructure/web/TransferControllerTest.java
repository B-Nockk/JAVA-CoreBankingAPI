// transfer-module/src/test/java/com/coreledger/transfer/infrastructure/web/TransferControllerTest.java
package com.coreledger.transfer.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.coreledger.shared.domain.Currency;
import com.coreledger.transfer.application.port.in.GetTransferUseCase;
import com.coreledger.transfer.application.port.in.InitiateTransferUseCase;
import com.coreledger.transfer.domain.exceptions.InvalidTransferException;
import com.coreledger.transfer.domain.exceptions.TransferNotFoundException;

/**
 * Web layer tests for TransferController.
 *
 * Same pattern as AccountControllerTest.
 * Requires TestTransferExceptionHandler in the same package (test sources)
 * to handle domain exceptions — GlobalExceptionHandler lives in app/ and
 * can't be loaded in this module slice.
 */
@WebMvcTest(TransferController.class)
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InitiateTransferUseCase initiateTransferUseCase;

    @MockBean
    private GetTransferUseCase getTransferUseCase;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private InitiateTransferUseCase.TransferResult transferResult(String status) {
        return new InitiateTransferUseCase.TransferResult(
                "transfer-uuid-001",
                "1234567890",
                "0987654321",
                new BigDecimal("500.00"),
                Currency.NGN,
                status,
                Instant.now());
    }

    private GetTransferUseCase.TransferResult getTransferResult() {
        return new GetTransferUseCase.TransferResult(
                "transfer-uuid-001",
                "1234567890",
                "0987654321",
                new BigDecimal("500.00"),
                Currency.NGN,
                "INITIATED",
                null,
                Instant.now());
    }

    // ── POST /api/v1/transfers ────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/transfers — initiate transfer")
    class InitiateTransfer {

        @Test
        @DisplayName("should return 201 with transfer details on valid request")
        void shouldReturn201OnValidRequest() throws Exception {
            when(initiateTransferUseCase.execute(any()))
                    .thenReturn(transferResult("INITIATED"));

            mockMvc.perform(post("/api/v1/transfers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "sourceAccountNumber": "1234567890",
                              "destinationAccountNumber": "0987654321",
                              "amount": 500.00
                            }
                            """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.transferId").value("transfer-uuid-001"))
                    .andExpect(jsonPath("$.sourceAccountNumber").value("1234567890"))
                    .andExpect(jsonPath("$.destinationAccountNumber").value("0987654321"))
                    .andExpect(jsonPath("$.status").value("INITIATED"));
        }

        @Test
        @DisplayName("should return 400 when sourceAccountNumber is missing")
        void shouldReturn400WhenSourceMissing() throws Exception {
            mockMvc.perform(post("/api/v1/transfers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "destinationAccountNumber": "0987654321",
                              "amount": 500.00
                            }
                            """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when destinationAccountNumber is missing")
        void shouldReturn400WhenDestinationMissing() throws Exception {
            mockMvc.perform(post("/api/v1/transfers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "sourceAccountNumber": "1234567890",
                              "amount": 500.00
                            }
                            """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when amount is zero")
        void shouldReturn400WhenAmountIsZero() throws Exception {
            mockMvc.perform(post("/api/v1/transfers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "sourceAccountNumber": "1234567890",
                              "destinationAccountNumber": "0987654321",
                              "amount": 0
                            }
                            """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when amount is negative")
        void shouldReturn400WhenAmountIsNegative() throws Exception {
            mockMvc.perform(post("/api/v1/transfers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "sourceAccountNumber": "1234567890",
                              "destinationAccountNumber": "0987654321",
                              "amount": -100
                            }
                            """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 422 when transfer is invalid — e.g. inactive account")
        void shouldReturn422WhenTransferInvalid() throws Exception {
            when(initiateTransferUseCase.execute(any()))
                    .thenThrow(new InvalidTransferException("Source account is frozen"));

            mockMvc.perform(post("/api/v1/transfers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "sourceAccountNumber": "1234567890",
                              "destinationAccountNumber": "0987654321",
                              "amount": 500.00
                            }
                            """))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    // ── GET /api/v1/transfers/{transferId} ────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/transfers/{transferId} — get transfer")
    class GetTransfer {

        @Test
        @DisplayName("should return 200 with transfer details when found")
        void shouldReturn200WhenFound() throws Exception {
            when(getTransferUseCase.getById("transfer-uuid-001"))
                    .thenReturn(getTransferResult());

            mockMvc.perform(get("/api/v1/transfers/transfer-uuid-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.transferId").value("transfer-uuid-001"))
                    .andExpect(jsonPath("$.sourceAccountNumber").value("1234567890"))
                    .andExpect(jsonPath("$.status").value("INITIATED"));
        }

        @Test
        @DisplayName("should return 404 when transfer does not exist")
        void shouldReturn404WhenNotFound() throws Exception {
            when(getTransferUseCase.getById(anyString()))
                    .thenThrow(new TransferNotFoundException("non-existent-id"));

            mockMvc.perform(get("/api/v1/transfers/non-existent-id"))
                    .andExpect(status().isNotFound());
        }
    }
}