package com.miniwallet.wallet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import com.miniwallet.wallet.security.WithMockJwtUser;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.miniwallet.wallet.config.SecurityConfig;
import com.miniwallet.wallet.dto.AccountResponse;
import com.miniwallet.wallet.dto.AmountRequest;
import com.miniwallet.wallet.dto.CreateAccountRequest;
import com.miniwallet.wallet.dto.TransactionResponse;
import com.miniwallet.wallet.entity.TransactionType;
import com.miniwallet.wallet.exception.AccountNotFoundException;
import com.miniwallet.wallet.exception.InsufficientFundsException;
import com.miniwallet.wallet.service.AccountService;

/** Sadece web katmani: HTTP kodlari, dogrulama ve hata govdeleri. Servis mock'tur. */
@WebMvcTest(AccountController.class)
@Import(SecurityConfig.class)
@WithMockJwtUser(userId = 7)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private AccountService accountService;

    private static TransactionResponse transaction(TransactionType type, String amount, String balanceAfter) {
        return new TransactionResponse(10L, 1L, type, new BigDecimal(amount),
                new BigDecimal(balanceAfter), Instant.parse("2026-01-01T10:00:00Z"));
    }

    // ---- hesap acma ----

    @Test
    void createAccount_returns201WithBody() throws Exception {
        when(accountService.create(eq(7L), any(CreateAccountRequest.class))).thenReturn(
                new AccountResponse(1L, "Ayse", BigDecimal.ZERO, Instant.parse("2026-01-01T10:00:00Z")));

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerName\":\"Ayse\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.ownerName").value("Ayse"))
                .andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    void createAccount_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerName\":\"  \"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(accountService);
    }

    @Test
    void createAccount_tooLongName_returns400() throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerName\":\"" + "a".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAccount_missingBody_returns400() throws Exception {
        mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ---- yatirma / cekme ----

    @Test
    void deposit_returns201WithTransaction() throws Exception {
        when(accountService.deposit(eq(7L), eq(1L), any(AmountRequest.class)))
                .thenReturn(transaction(TransactionType.DEPOSIT, "100.50", "100.50"));

        mockMvc.perform(post("/api/accounts/1/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100.50}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(100.50))
                .andExpect(jsonPath("$.balanceAfter").value(100.50));
    }

    @Test
    void deposit_invalidAmounts_return400() throws Exception {
        // sifir, negatif, eksik alan ve ikiden fazla ondalik basamak
        for (String body : List.of("{\"amount\":0}", "{\"amount\":-5}", "{}", "{\"amount\":1.001}")) {
            mockMvc.perform(post("/api/accounts/1/deposit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(accountService);
    }

    @Test
    void deposit_unknownAccount_returns404WithProblemDetail() throws Exception {
        when(accountService.deposit(eq(7L), eq(99L), any(AmountRequest.class)))
                .thenThrow(new AccountNotFoundException(99L));

        mockMvc.perform(post("/api/accounts/99/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Hesap bulunamadi: 99"));
    }

    @Test
    void withdraw_insufficientFunds_returns422() throws Exception {
        when(accountService.withdraw(eq(7L), eq(1L), any(AmountRequest.class)))
                .thenThrow(new InsufficientFundsException(1L));

        mockMvc.perform(post("/api/accounts/1/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":500}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.detail").value("Yetersiz bakiye. Hesap: 1"));
    }

    // ---- islem gecmisi ----

    @Test
    void transactions_usesDefaultPaging() throws Exception {
        when(accountService.getTransactions(7L, 1L, 0, 20)).thenReturn(new PagedModel<>(new PageImpl<>(
                List.of(transaction(TransactionType.DEPOSIT, "5.00", "5.00")),
                PageRequest.of(0, 20), 1)));

        mockMvc.perform(get("/api/accounts/1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.page.size").value(20));

        verify(accountService).getTransactions(7L, 1L, 0, 20);
    }

    @Test
    void transactions_invalidPagingParameters_return400() throws Exception {
        for (String query : List.of("?size=0", "?size=101", "?page=-1")) {
            mockMvc.perform(get("/api/accounts/1/transactions" + query))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(accountService);
    }

    @Test
    void transactions_unknownAccount_returns404() throws Exception {
        when(accountService.getTransactions(7L, 99L, 0, 20)).thenThrow(new AccountNotFoundException(99L));

        mockMvc.perform(get("/api/accounts/99/transactions"))
                .andExpect(status().isNotFound());
    }
}
