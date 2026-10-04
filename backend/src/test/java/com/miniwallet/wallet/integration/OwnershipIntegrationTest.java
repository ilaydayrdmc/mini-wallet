package com.miniwallet.wallet.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.miniwallet.wallet.TestcontainersConfig;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Iki gercek kullanici, gercek HTTP istekleri ve gercek token'larla: herkes yalnizca kendi
 * hesaplarini gorur ve kullanir; baskasinin hesabi "bulunamadi" (404) gibi davranir.
 */
@Import(TestcontainersConfig.class)
@SpringBootTest
@AutoConfigureMockMvc
class OwnershipIntegrationTest {

    private static final String PASSWORD = "gizli-sifre-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /** Kayit olur, giris yapar ve Authorization basligi degerini dondurur. */
    private String signUp(String username) throws Exception {
        String credentials = "{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isCreated());
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(body).get("accessToken").asString();
    }

    private long createAccount(String auth, String name) throws Exception {
        String body = mockMvc.perform(post("/api/accounts")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerName\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return json.get("id").asLong();
    }

    private ResultActions postJson(String auth, String url, String json) throws Exception {
        return mockMvc.perform(post(url).header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void userSeesOnlyOwnAccounts() throws Exception {
        String ayse = signUp("sahiplik.ayse");
        String mehmet = signUp("sahiplik.mehmet");
        createAccount(ayse, "Ayse maas");
        createAccount(ayse, "Ayse birikim");
        createAccount(mehmet, "Mehmet");

        mockMvc.perform(get("/api/accounts").header(HttpHeaders.AUTHORIZATION, ayse))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].ownerName").value("Ayse maas"));

        mockMvc.perform(get("/api/accounts").header(HttpHeaders.AUTHORIZATION, mehmet))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ownerName").value("Mehmet"));
    }

    @Test
    void userCannotTouchSomeoneElsesAccount() throws Exception {
        String ayse = signUp("yetki.ayse");
        String mehmet = signUp("yetki.mehmet");
        long ayseAccount = createAccount(ayse, "Ayse");
        postJson(ayse, "/api/accounts/" + ayseAccount + "/deposit", "{\"amount\":100}")
                .andExpect(status().isCreated());

        // Mehmet, Ayse'nin hesabina yatirma/cekme/gecmis denemesi: hepsi 404
        postJson(mehmet, "/api/accounts/" + ayseAccount + "/deposit", "{\"amount\":1}")
                .andExpect(status().isNotFound());
        postJson(mehmet, "/api/accounts/" + ayseAccount + "/withdraw", "{\"amount\":1}")
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/accounts/" + ayseAccount + "/transactions")
                        .header(HttpHeaders.AUTHORIZATION, mehmet))
                .andExpect(status().isNotFound());

        // Mehmet, Ayse'nin hesabindan para cekmek icin transfer baslatamaz
        long mehmetAccount = createAccount(mehmet, "Mehmet");
        postJson(mehmet, "/api/transfers",
                "{\"fromAccountId\":" + ayseAccount + ",\"toAccountId\":" + mehmetAccount + ",\"amount\":50}")
                .andExpect(status().isNotFound());

        // Ayse'nin bakiyesi ve gecmisi degismedi
        mockMvc.perform(get("/api/accounts").header(HttpHeaders.AUTHORIZATION, ayse))
                .andExpect(jsonPath("$[0].balance").value(100.00));
        mockMvc.perform(get("/api/accounts/" + ayseAccount + "/transactions")
                        .header(HttpHeaders.AUTHORIZATION, ayse))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void userCanSendMoneyToSomeoneElsesAccount() throws Exception {
        String ayse = signUp("gonder.ayse");
        String mehmet = signUp("gonder.mehmet");
        long ayseAccount = createAccount(ayse, "Ayse");
        long mehmetAccount = createAccount(mehmet, "Mehmet");
        postJson(ayse, "/api/accounts/" + ayseAccount + "/deposit", "{\"amount\":100}")
                .andExpect(status().isCreated());

        postJson(ayse, "/api/transfers",
                "{\"fromAccountId\":" + ayseAccount + ",\"toAccountId\":" + mehmetAccount + ",\"amount\":30}")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/accounts").header(HttpHeaders.AUTHORIZATION, ayse))
                .andExpect(jsonPath("$[0].balance").value(70.00));
        mockMvc.perform(get("/api/accounts").header(HttpHeaders.AUTHORIZATION, mehmet))
                .andExpect(jsonPath("$[0].balance").value(30.00));
        // Gelen transfer Mehmet'in kendi gecmisinde gorunur
        mockMvc.perform(get("/api/accounts/" + mehmetAccount + "/transactions")
                        .header(HttpHeaders.AUTHORIZATION, mehmet))
                .andExpect(jsonPath("$.content[0].type").value("TRANSFER_IN"));

        assertThat(ayseAccount).isNotEqualTo(mehmetAccount);
    }
}
