package com.miniwallet.wallet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.miniwallet.wallet.dto.TransactionResponse;
import com.miniwallet.wallet.dto.TransferRequest;
import com.miniwallet.wallet.entity.TransactionType;
import com.miniwallet.wallet.exception.AccountNotFoundException;
import com.miniwallet.wallet.exception.InsufficientFundsException;
import com.miniwallet.wallet.exception.InvalidTransferException;
import com.miniwallet.wallet.service.TransferService;

@WebMvcTest(TransferController.class)
class TransferControllerTest {

    private static final String VALID_BODY = "{\"fromAccountId\":1,\"toAccountId\":2,\"amount\":25}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransferService transferService;

    private ResultActions postTransfer(String body) throws Exception {
        return mockMvc.perform(post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void transfer_returns201WithOutgoingTransaction() throws Exception {
        when(transferService.transfer(any(TransferRequest.class))).thenReturn(new TransactionResponse(
                7L, 1L, TransactionType.TRANSFER_OUT, new BigDecimal("25.00"),
                new BigDecimal("75.00"), Instant.parse("2026-01-01T10:00:00Z")));

        postTransfer(VALID_BODY)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("TRANSFER_OUT"))
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.balanceAfter").value(75.00));
    }

    @Test
    void transfer_invalidBodies_return400() throws Exception {
        List<String> bodies = List.of(
                "{\"toAccountId\":2,\"amount\":25}",
                "{\"fromAccountId\":1,\"amount\":25}",
                "{\"fromAccountId\":1,\"toAccountId\":2}",
                "{\"fromAccountId\":1,\"toAccountId\":2,\"amount\":0}",
                "{\"fromAccountId\":1,\"toAccountId\":2,\"amount\":-1}");
        for (String body : bodies) {
            postTransfer(body).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(transferService);
    }

    @Test
    void transfer_toSameAccount_returns400WithDetail() throws Exception {
        when(transferService.transfer(any(TransferRequest.class)))
                .thenThrow(new InvalidTransferException("Kendi hesabiniza transfer yapamazsiniz"));

        postTransfer("{\"fromAccountId\":1,\"toAccountId\":1,\"amount\":5}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Kendi hesabiniza transfer yapamazsiniz"));
    }

    @Test
    void transfer_unknownAccount_returns404() throws Exception {
        when(transferService.transfer(any(TransferRequest.class)))
                .thenThrow(new AccountNotFoundException(2L));

        postTransfer(VALID_BODY).andExpect(status().isNotFound());
    }

    @Test
    void transfer_insufficientFunds_returns422() throws Exception {
        when(transferService.transfer(any(TransferRequest.class)))
                .thenThrow(new InsufficientFundsException(1L));

        postTransfer(VALID_BODY)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }
}
