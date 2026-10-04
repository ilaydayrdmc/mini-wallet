package com.miniwallet.wallet.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.miniwallet.wallet.config.SecurityConfig;
import com.miniwallet.wallet.service.AccountService;
import com.miniwallet.wallet.service.AuthService;
import com.miniwallet.wallet.service.TransferService;

/** Hangi yollar herkese acik, hangileri token istiyor? Gercek SecurityConfig ile, JwtDecoder mock. */
@WebMvcTest({AccountController.class, TransferController.class, AuthController.class, HelloController.class})
@Import(SecurityConfig.class)
class SecurityRulesTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private TransferService transferService;

    @MockitoBean
    private AuthService authService;

    private static Jwt validJwt() {
        return new Jwt("gecerli-token", Instant.now(), Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"), Map.of("sub", "1"));
    }

    @Test
    void publicEndpoints_doNotRequireToken() throws Exception {
        mockMvc.perform(get("/api/hello")).andExpect(status().isOk());

        // Bos govde: guvenlikten gecip dogrulamada 400 almak, 401 olmadigini gosterir
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpoints_withoutToken_return401() throws Exception {
        mockMvc.perform(get("/api/accounts")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/accounts/1/transactions")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"ownerName\":\"Ayse\"}")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/accounts/1/deposit").contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":10}")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/accounts/1/withdraw").contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":10}")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromAccountId\":1,\"toAccountId\":2,\"amount\":10}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidToken_returns401() throws Exception {
        when(jwtDecoder.decode(anyString())).thenThrow(new BadJwtException("imza gecersiz"));

        mockMvc.perform(get("/api/accounts").header(HttpHeaders.AUTHORIZATION, "Bearer sahte.token.degeri"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validToken_isAccepted() throws Exception {
        when(jwtDecoder.decode("gecerli-token")).thenReturn(validJwt());

        mockMvc.perform(get("/api/accounts").header(HttpHeaders.AUTHORIZATION, "Bearer gecerli-token"))
                .andExpect(status().isOk());
    }

    @Test
    void basicAuthHeader_isNotAccepted() throws Exception {
        mockMvc.perform(get("/api/accounts").header(HttpHeaders.AUTHORIZATION, "Basic YXlzZTpzaWZyZQ=="))
                .andExpect(status().isUnauthorized());
    }
}
