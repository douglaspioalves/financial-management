package com.gastos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gastos.domain.Card;
import com.gastos.domain.Person;
import com.gastos.repository.CardRepository;
import com.gastos.repository.PersonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private CardRepository cardRepository;

    private static String bearerToken;
    private static String createdCardId;
    private static Long   createdCardVersion;
    private static UUID   testPersonId;

    @BeforeEach
    void ensureTestPerson() {
        if (testPersonId == null) {
            Person person = personRepository.save(
                    Person.builder().name("Titular Teste").color("#4a7fc4").build());
            testPersonId = person.getId();
        }
    }

    // -------------------------------------------------------------------------
    // Helper: obtém Bearer token
    // -------------------------------------------------------------------------

    private String obtainToken() throws Exception {
        if (bearerToken != null) return bearerToken;

        Map<String, String> register = Map.of(
                "name", "QA Cards",
                "email", "qa-cards@example.com",
                "password", "senha12345");
        MvcResult result;
        try {
            result = mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(register)))
                    .andExpect(status().isCreated())
                    .andReturn();
        } catch (AssertionError e) {
            result = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("email", "qa-cards@example.com", "password", "senha12345"))))
                    .andExpect(status().isOk())
                    .andReturn();
        }
        bearerToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
        return bearerToken;
    }

    // -------------------------------------------------------------------------
    // GET /api/cards — sem token → 401
    // -------------------------------------------------------------------------

    @Test @Order(1)
    @DisplayName("GET /api/cards sem token retorna 401")
    void listCards_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/cards"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // GET /api/cards — com token → 200
    // -------------------------------------------------------------------------

    @Test @Order(2)
    @DisplayName("GET /api/cards com token válido retorna 200")
    void listCards_withValidToken_returns200() throws Exception {
        mockMvc.perform(get("/api/cards")
                        .header("Authorization", "Bearer " + obtainToken()))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // POST /api/cards — dados válidos → 201
    // -------------------------------------------------------------------------

    @Test @Order(3)
    @DisplayName("POST /api/cards com dados válidos retorna 201")
    void createCard_withValidData_returns201() throws Exception {
        Map<String, Object> body = Map.of(
                "name", "Nubank Teste",
                "ownerPersonId", testPersonId.toString(),
                "closingDay", 10,
                "dueDay", 17);

        MvcResult result = mockMvc.perform(post("/api/cards")
                        .header("Authorization", "Bearer " + obtainToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Nubank Teste"))
                .andExpect(jsonPath("$.ownerPersonName").value("Titular Teste"))
                .andExpect(jsonPath("$.closingDay").value(10))
                .andExpect(jsonPath("$.dueDay").value(17))
                .andReturn();

        var node = objectMapper.readTree(result.getResponse().getContentAsString());
        createdCardId      = node.get("id").asText();
        createdCardVersion = node.get("version").asLong();
        assertThat(createdCardId).isNotBlank();
    }

    // -------------------------------------------------------------------------
    // POST /api/cards — closingDay inválido → 400
    // -------------------------------------------------------------------------

    @Test @Order(4)
    @DisplayName("POST /api/cards com closingDay=0 retorna 400")
    void createCard_withInvalidClosingDay_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "name", "Cartão Inválido",
                "ownerPersonId", testPersonId.toString(),
                "closingDay", 0,
                "dueDay", 17);

        mockMvc.perform(post("/api/cards")
                        .header("Authorization", "Bearer " + obtainToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.closingDay").isNotEmpty());
    }

    // -------------------------------------------------------------------------
    // POST /api/cards — dueDay inválido → 400
    // -------------------------------------------------------------------------

    @Test @Order(5)
    @DisplayName("POST /api/cards com dueDay=32 retorna 400")
    void createCard_withInvalidDueDay_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "name", "Cartão Inválido",
                "ownerPersonId", testPersonId.toString(),
                "closingDay", 10,
                "dueDay", 32);

        mockMvc.perform(post("/api/cards")
                        .header("Authorization", "Bearer " + obtainToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.dueDay").isNotEmpty());
    }

    // -------------------------------------------------------------------------
    // POST /api/cards — sem nome → 400
    // -------------------------------------------------------------------------

    @Test @Order(6)
    @DisplayName("POST /api/cards sem nome retorna 400")
    void createCard_withoutName_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "ownerPersonId", testPersonId.toString(),
                "closingDay", 10,
                "dueDay", 17);

        mockMvc.perform(post("/api/cards")
                        .header("Authorization", "Bearer " + obtainToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").isNotEmpty());
    }

    // -------------------------------------------------------------------------
    // GET /api/cards/{id} — id válido → 200
    // -------------------------------------------------------------------------

    @Test @Order(7)
    @DisplayName("GET /api/cards/{id} com ID válido retorna 200")
    void getCard_withValidId_returns200() throws Exception {
        assertThat(createdCardId).isNotNull();

        mockMvc.perform(get("/api/cards/" + createdCardId)
                        .header("Authorization", "Bearer " + obtainToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdCardId));
    }

    // -------------------------------------------------------------------------
    // GET /api/cards/{id} — id desconhecido → 404
    // -------------------------------------------------------------------------

    @Test @Order(8)
    @DisplayName("GET /api/cards/{id} com ID inexistente retorna 404")
    void getCard_withUnknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/cards/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + obtainToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").isNotEmpty());
    }

    // -------------------------------------------------------------------------
    // PUT /api/cards/{id} — dados válidos → 200
    // -------------------------------------------------------------------------

    @Test @Order(9)
    @DisplayName("PUT /api/cards/{id} com dados válidos retorna 200")
    void updateCard_withValidData_returns200() throws Exception {
        assertThat(createdCardId).isNotNull();

        Map<String, Object> body = Map.of(
                "name", "Nubank Atualizado",
                "ownerPersonId", testPersonId.toString(),
                "closingDay", 15,
                "dueDay", 22);

        mockMvc.perform(put("/api/cards/" + createdCardId)
                        .header("Authorization", "Bearer " + obtainToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nubank Atualizado"))
                .andExpect(jsonPath("$.closingDay").value(15));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/cards/{id} — sem transações → 204
    // -------------------------------------------------------------------------

    @Test @Order(10)
    @DisplayName("DELETE /api/cards/{id} sem transações vinculadas retorna 204")
    void deleteCard_withNoTransactions_returns204() throws Exception {
        assertThat(createdCardId).isNotNull();

        mockMvc.perform(delete("/api/cards/" + createdCardId)
                        .header("Authorization", "Bearer " + obtainToken()))
                .andExpect(status().isNoContent());
    }
}
