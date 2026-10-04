package com.miniwallet.wallet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.miniwallet.wallet.dto.LoginRequest;
import com.miniwallet.wallet.dto.RegisterRequest;
import com.miniwallet.wallet.dto.TokenResponse;
import com.miniwallet.wallet.dto.UserResponse;
import com.miniwallet.wallet.exception.InvalidCredentialsException;
import com.miniwallet.wallet.exception.UsernameTakenException;
import com.miniwallet.wallet.service.AuthService;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    private ResultActions register(String body) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void register_returns201WithoutAnyPasswordField() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(new UserResponse(1L, "ayse", Instant.parse("2026-01-01T10:00:00Z")));

        register("{\"username\":\"ayse\",\"password\":\"gizli-sifre-123\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("ayse"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void register_invalidBodies_return400() throws Exception {
        List<String> bodies = List.of(
                "{\"password\":\"gizli-sifre-123\"}",
                "{\"username\":\"ayse\"}",
                "{\"username\":\"ab\",\"password\":\"gizli-sifre-123\"}",
                "{\"username\":\"ayse kaya\",\"password\":\"gizli-sifre-123\"}",
                "{\"username\":\"ayse\",\"password\":\"kisa\"}",
                "{\"username\":\"ayse\",\"password\":\"" + "x".repeat(73) + "\"}");
        for (String body : bodies) {
            register(body).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(authService);
    }

    private ResultActions login(String body) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void login_returns200WithToken() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new TokenResponse("jwt-degeri", "Bearer", 3600));

        login("{\"username\":\"ayse\",\"password\":\"gizli-sifre-123\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-degeri"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    void login_badCredentials_returns401() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException());

        login("{\"username\":\"ayse\",\"password\":\"yanlis\"}")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Kullanici adi veya sifre hatali"));
    }

    @Test
    void login_missingFields_return400() throws Exception {
        for (String body : List.of("{}", "{\"username\":\"ayse\"}", "{\"password\":\"x\"}",
                "{\"username\":\"\",\"password\":\"x\"}")) {
            login(body).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(authService);
    }

    @Test
    void register_usernameTaken_returns409() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new UsernameTakenException("ayse"));

        register("{\"username\":\"ayse\",\"password\":\"gizli-sifre-123\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
