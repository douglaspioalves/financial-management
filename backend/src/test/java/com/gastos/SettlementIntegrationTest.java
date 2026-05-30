package com.gastos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gastos.domain.Card;
import com.gastos.domain.Category;
import com.gastos.domain.CategoryType;
import com.gastos.domain.Person;
import com.gastos.repository.CardRepository;
import com.gastos.repository.CategoryRepository;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração dos endpoints de acerto de contas (Settlement).
 *
 * Contrato:  GET /api/settlement?month=yyyy-MM
 *
 * Fixtures:
 *   - "Alice IT" (personA — menor nome alfabético) e "Bob IT" (personB)
 *   - Categoria de despesa "Settlement IT Cat"
 *   - Cartão com closingDay=10, dueDay=17 (titular: Alice IT) — usado em TC-IT-06
 *
 * Meses usados por teste (sem sobreposição):
 *   TC-IT-02  2030-01  (futuro, garantidamente vazio)
 *   TC-IT-03  mês corrente (sem param)
 *   TC-IT-04  2026-06
 *   TC-IT-05  2026-07
 *   TC-IT-06  2026-09 (parcela 1) / 2026-08 (deve ser vazio)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SettlementIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PersonRepository personRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private CardRepository cardRepository;

    private static String bearerToken;
    private static UUID personAId;
    private static UUID personBId;
    private static UUID expenseCategoryId;
    private static UUID testCardId;

    /**
     * Garante que os fixtures existem antes de cada teste.
     * Usa campos static para criar uma única vez por contexto de aplicação.
     * "Alice IT" < "Bob IT" alfabeticamente → personA=Alice, personB=Bob
     * (mesmo critério do SettlementService).
     */
    @BeforeEach
    void ensureFixtures() {
        if (personAId == null) {
            Person alice = personRepository.save(
                    Person.builder().name("Alice IT").color("#4a7fc4").version(0L).build());
            personAId = alice.getId();
        }
        if (personBId == null) {
            Person bob = personRepository.save(
                    Person.builder().name("Bob IT").color("#e8927c").version(0L).build());
            personBId = bob.getId();
        }
        if (expenseCategoryId == null) {
            Category cat = categoryRepository.save(
                    Category.builder()
                            .name("Settlement IT Cat")
                            .type(CategoryType.EXPENSE)
                            .color("#e88a74")
                            .version(0L)
                            .build());
            expenseCategoryId = cat.getId();
        }
        if (testCardId == null) {
            Person alice = personRepository.findById(personAId).orElseThrow();
            Card card = cardRepository.save(
                    Card.builder()
                            .name("IT Card")
                            .owner(alice)
                            .closingDay(10)
                            .dueDay(17)
                            .version(0L)
                            .build());
            testCardId = card.getId();
        }
    }

    private String obtainToken() throws Exception {
        if (bearerToken != null) return bearerToken;
        Map<String, String> register = Map.of(
                "name", "QA Settlement",
                "email", "qa-settlement@example.com",
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
                                    Map.of("email", "qa-settlement@example.com",
                                           "password", "senha12345"))))
                    .andExpect(status().isOk())
                    .andReturn();
        }
        bearerToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
        return bearerToken;
    }

    // =========================================================================
    // TC-IT-01: endpoint exige autenticação
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("GET /api/settlement sem token → 401 (autenticação obrigatória)")
    void getSettlement_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/settlement")
                        .param("month", "2026-05"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // TC-IT-02: mês vazio retorna acerto quitado
    // =========================================================================

    @Test
    @Order(2)
    @DisplayName("GET /api/settlement com token e sem dados → 200, settled=true, totalExpense=0")
    void getSettlement_withTokenAndNoData_returns200Settled() throws Exception {
        mockMvc.perform(get("/api/settlement")
                        .header("Authorization", "Bearer " + obtainToken())
                        .param("month", "2030-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settled").value(true))
                .andExpect(jsonPath("$.totalExpense").value(0))
                .andExpect(jsonPath("$.pendingProportional").value(false));
    }

    // =========================================================================
    // TC-IT-03: sem parâmetro month usa mês corrente (não retorna 400)
    // =========================================================================

    @Test
    @Order(3)
    @DisplayName("GET /api/settlement sem parâmetro month → 200 (usa mês corrente por padrão)")
    void getSettlement_withoutMonthParam_returns200WithCurrentMonth() throws Exception {
        mockMvc.perform(get("/api/settlement")
                        .header("Authorization", "Bearer " + obtainToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settled").isBoolean())
                .andExpect(jsonPath("$.totalExpense").isNumber());
    }

    // =========================================================================
    // TC-IT-04: fluxo completo FIFTY_FIFTY — PersonB deve R$500 a PersonA
    // =========================================================================

    @Test
    @Order(4)
    @DisplayName("GET /api/settlement — FIFTY_FIFTY R$1000 pago por Alice → Bob deve R$500 a Alice")
    void getSettlement_fullFlow_fiftyFifty() throws Exception {
        Map<String, Object> tx = new HashMap<>();
        tx.put("type", "EXPENSE");
        tx.put("amount", 1000.00);
        tx.put("date", "2026-06-15");
        tx.put("categoryId", expenseCategoryId.toString());
        tx.put("paidByPersonId", personAId.toString());
        tx.put("paymentMethod", "CASH");
        tx.put("splitRule", "FIFTY_FIFTY");
        tx.put("installmentsTotal", 1);
        tx.put("description", "IT04 despesa FIFTY_FIFTY");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + obtainToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tx)))
                .andExpect(status().isCreated());

        // Alice pagou R$1000, cada um deveria pagar R$500 → Bob (PERSON_B) deve R$500 a Alice (PERSON_A)
        mockMvc.perform(get("/api/settlement")
                        .header("Authorization", "Bearer " + obtainToken())
                        .param("month", "2026-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settled").value(false))
                .andExpect(jsonPath("$.totalExpense").value(1000.0))
                .andExpect(jsonPath("$.debtor").value("PERSON_B"))
                .andExpect(jsonPath("$.creditor").value("PERSON_A"))
                .andExpect(jsonPath("$.amountOwed").value(500.0));
    }

    // =========================================================================
    // TC-IT-05: despesa PROPORTIONAL sem receitas → pendência
    // =========================================================================

    @Test
    @Order(5)
    @DisplayName("GET /api/settlement — despesa PROPORTIONAL sem receitas → pendingProportional=true")
    void getSettlement_proportionalWithoutIncome_returnsPending() throws Exception {
        Map<String, Object> tx = new HashMap<>();
        tx.put("type", "EXPENSE");
        tx.put("amount", 800.00);
        tx.put("date", "2026-07-15");
        tx.put("categoryId", expenseCategoryId.toString());
        tx.put("paidByPersonId", personAId.toString());
        tx.put("paymentMethod", "CASH");
        tx.put("splitRule", "PROPORTIONAL");
        tx.put("installmentsTotal", 1);
        tx.put("description", "IT05 despesa PROPORTIONAL");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + obtainToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tx)))
                .andExpect(status().isCreated());

        // Sem receitas individuais em julho/2026 → amountOwed=null, pendingMessage preenchida
        mockMvc.perform(get("/api/settlement")
                        .header("Authorization", "Bearer " + obtainToken())
                        .param("month", "2026-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingProportional").value(true))
                .andExpect(jsonPath("$.amountOwed").value((Object) null))
                .andExpect(jsonPath("$.pendingMessage").isNotEmpty());
    }

    // =========================================================================
    // TC-IT-06: parcela de cartão entra pelo referenceMonth, não pela data da compra
    // =========================================================================

    @Test
    @Order(6)
    @DisplayName("GET /api/settlement — parcela entra pelo referenceMonth (não data da compra)")
    void getSettlement_installmentEntersByReferenceMonth() throws Exception {
        // Cartão closingDay=10 → compra 2026-09-05 (dia 5 < 10) → parcela 1 em set/2026
        // R$900 ÷ 3 = R$300 por parcela (exato, sem arredondamento)
        Map<String, Object> tx = new HashMap<>();
        tx.put("type", "EXPENSE");
        tx.put("amount", 900.00);
        tx.put("date", "2026-09-05");
        tx.put("categoryId", expenseCategoryId.toString());
        tx.put("paidByPersonId", personAId.toString());
        tx.put("paymentMethod", "CREDIT");
        tx.put("splitRule", "FIFTY_FIFTY");
        tx.put("installmentsTotal", 3);
        tx.put("cardId", testCardId.toString());
        tx.put("description", "IT06 compra parcelada 3x R$900");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + obtainToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tx)))
                .andExpect(status().isCreated());

        // Setembro/2026: apenas a 1ª parcela (R$300) — não o valor cheio R$900
        mockMvc.perform(get("/api/settlement")
                        .header("Authorization", "Bearer " + obtainToken())
                        .param("month", "2026-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense").value(300.0));

        // Agosto/2026: nenhuma parcela (compra feita em setembro)
        mockMvc.perform(get("/api/settlement")
                        .header("Authorization", "Bearer " + obtainToken())
                        .param("month", "2026-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense").value(0));
    }
}
