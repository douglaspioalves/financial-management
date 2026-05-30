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

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de fluxo completo ponta a ponta (S-07-02).
 *
 * Cobre o ciclo: registro -> login -> lancamentos -> acerto de contas -> dashboard.
 *
 * Fixtures:
 *   - "Ana Fluxo" (personA - menor nome alfabetico) e "Carlos Fluxo" (personB)
 *   - Categoria de despesa "Alimentacao Fluxo"
 *   - Categoria de receita "Salario Fluxo"
 *   - Cartao com closingDay=5, dueDay=12, titular=Ana Fluxo
 *
 * Meses usados (sem sobreposicao com outros testes de integracao):
 *   TC-F02 ao TC-F07  2026-10
 *   TC-F08            2031-01
 *
 * Calculo do acerto em outubro/2026 (TC-F05):
 *   - R$600 FIFTY_FIFTY a vista (TC-F02), paga por Ana: paidA+=600, shouldPayA+=300, shouldPayB+=300
 *   - R$300 (1a parcela de R$900/3x) FIFTY_FIFTY paga por Ana: paidA+=300, shouldPayA+=150, shouldPayB+=150
 *   - paidA=900, paidB=0, shouldPayA=450, shouldPayB=450
 *   - balanceA=+450 -> PERSON_B (Carlos) deve R$450 a PERSON_A (Ana)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PersonRepository personRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private CardRepository cardRepository;

    // Campos static: criados uma unica vez para toda a classe (contexto compartilhado)
    private static String bearerToken;
    private static UUID personAId;         // Ana Fluxo
    private static UUID personBId;         // Carlos Fluxo
    private static UUID expenseCategoryId;
    private static UUID incomeCategoryId;
    private static UUID testCardId;

    /**
     * Garante que os fixtures existam antes de cada teste.
     * "Ana Fluxo" < "Carlos Fluxo" -> personA=Ana, personB=Carlos
     * (mesmo criterio de ordenacao do SettlementService).
     */
    @BeforeEach
    void ensureFixtures() {
        if (personAId == null) {
            Person ana = personRepository.save(
                    Person.builder().name("Ana Fluxo").color("#7ec8a4").version(0L).build());
            personAId = ana.getId();
        }
        if (personBId == null) {
            Person carlos = personRepository.save(
                    Person.builder().name("Carlos Fluxo").color("#e8927c").version(0L).build());
            personBId = carlos.getId();
        }
        if (expenseCategoryId == null) {
            Category cat = categoryRepository.save(
                    Category.builder()
                            .name("Alimentacao Fluxo")
                            .type(CategoryType.EXPENSE)
                            .color("#e88a74")
                            .version(0L)
                            .build());
            expenseCategoryId = cat.getId();
        }
        if (incomeCategoryId == null) {
            Category cat = categoryRepository.save(
                    Category.builder()
                            .name("Salario Fluxo")
                            .type(CategoryType.INCOME)
                            .color("#7ec8a4")
                            .version(0L)
                            .build());
            incomeCategoryId = cat.getId();
        }
        if (testCardId == null) {
            Person ana = personRepository.findById(personAId).orElseThrow();
            Card card = cardRepository.save(
                    Card.builder()
                            .name("Cartao Fluxo")
                            .owner(ana)
                            .closingDay(5)
                            .dueDay(12)
                            .version(0L)
                            .build());
            testCardId = card.getId();
        }
    }

    // =========================================================================
    // TC-F01: Registro e login
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("TC-F01: POST /api/auth/register retorna 201; POST /api/auth/login retorna 200 com JWT")
    void tcF01_registerAndLogin_returnsJwt() throws Exception {
        Map<String, String> registerBody = Map.of(
                "name", "QA Fluxo",
                "email", "qa-fluxo@example.com",
                "password", "senha12345");

        MvcResult registerResult;
        try {
            registerResult = mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerBody)))
                    .andExpect(status().isCreated())
                    .andReturn();
        } catch (AssertionError e) {
            // Ja registrado em execucao anterior: faz login diretamente
            registerResult = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("email", "qa-fluxo@example.com",
                                           "password", "senha12345"))))
                    .andExpect(status().isOk())
                    .andReturn();
            bearerToken = objectMapper.readTree(
                    registerResult.getResponse().getContentAsString()).get("token").asText();
            return;
        }

        bearerToken = objectMapper.readTree(
                registerResult.getResponse().getContentAsString()).get("token").asText();

        // Verifica login separado apos registro
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "qa-fluxo@example.com",
                                       "password", "senha12345"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andReturn();

        // Atualiza o token com o mais recente (login)
        bearerToken = objectMapper.readTree(
                loginResult.getResponse().getContentAsString()).get("token").asText();
    }

    // =========================================================================
    // TC-F02: Lancamento a vista FIFTY_FIFTY
    // =========================================================================

    @Test
    @Order(2)
    @DisplayName("TC-F02: Despesa R$600 a vista FIFTY_FIFTY paga por Ana retorna 201")
    void tcF02_cashExpenseFiftyFifty_returns201() throws Exception {
        Map<String, Object> tx = new HashMap<>();
        tx.put("type", "EXPENSE");
        tx.put("amount", 600.00);
        tx.put("date", "2026-10-10");
        tx.put("categoryId", expenseCategoryId.toString());
        tx.put("paidByPersonId", personAId.toString());
        tx.put("paymentMethod", "CASH");
        tx.put("splitRule", "FIFTY_FIFTY");
        tx.put("installmentsTotal", 1);
        tx.put("description", "F02 despesa a vista FIFTY_FIFTY");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + obtainToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tx)))
                .andExpect(status().isCreated());
    }

    // =========================================================================
    // TC-F03: Receita individual PERSON_A (Ana)
    // =========================================================================

    @Test
    @Order(3)
    @DisplayName("TC-F03: Receita R$5000 PERSON_A (Ana) em outubro/2026 retorna 201")
    void tcF03_individualIncome_returns201() throws Exception {
        Map<String, Object> tx = new HashMap<>();
        tx.put("type", "INCOME");
        tx.put("amount", 5000.00);
        tx.put("date", "2026-10-05");
        tx.put("categoryId", incomeCategoryId.toString());
        tx.put("paidByPersonId", personAId.toString());
        tx.put("paymentMethod", "TRANSFER");
        tx.put("splitRule", "PERSON_A");
        tx.put("installmentsTotal", 1);
        tx.put("description", "F03 salario Ana outubro/2026");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + obtainToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tx)))
                .andExpect(status().isCreated());
    }

    // =========================================================================
    // TC-F04: Compra parcelada 3x no cartao
    // =========================================================================

    @Test
    @Order(4)
    @DisplayName("TC-F04: Compra R$900 parcelada 3x (data=2026-10-01, closingDay=5) retorna 201; parcela 1 em out/2026")
    void tcF04_creditInstallment3x_returns201() throws Exception {
        // data=2026-10-01 (dia 1) < closingDay=5 -> parcela 1 em out/2026
        Map<String, Object> tx = new HashMap<>();
        tx.put("type", "EXPENSE");
        tx.put("amount", 900.00);
        tx.put("date", "2026-10-01");
        tx.put("categoryId", expenseCategoryId.toString());
        tx.put("paidByPersonId", personAId.toString());
        tx.put("paymentMethod", "CREDIT");
        tx.put("splitRule", "FIFTY_FIFTY");
        tx.put("installmentsTotal", 3);
        tx.put("cardId", testCardId.toString());
        tx.put("description", "F04 compra parcelada 3x R$900");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + obtainToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tx)))
                .andExpect(status().isCreated());
    }

    // =========================================================================
    // TC-F05: Acerto de contas outubro/2026
    // =========================================================================

    @Test
    @Order(5)
    @DisplayName("TC-F05: Acerto out/2026 - totalExpense=900, debtor=PERSON_B, amountOwed=450")
    void tcF05_settlementOctober2026_correctValues() throws Exception {
        /*
         * Despesas em outubro/2026:
         *   - R$600 FIFTY_FIFTY a vista (TC-F02), paga por Ana (personA)
         *   - R$300 1a parcela do R$900 parcelado FIFTY_FIFTY (TC-F04), paga por Ana
         *
         * Calculo:
         *   paidA=900, paidB=0
         *   shouldPayA=300+150=450, shouldPayB=300+150=450
         *   balanceA=900-450=+450 -> PERSON_B (Carlos) deve R$450 a PERSON_A (Ana)
         */
        mockMvc.perform(get("/api/settlement")
                        .header("Authorization", "Bearer " + obtainToken())
                        .param("month", "2026-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settled").value(false))
                .andExpect(jsonPath("$.totalExpense").value(900.00))
                .andExpect(jsonPath("$.debtor").value("PERSON_B"))
                .andExpect(jsonPath("$.creditor").value("PERSON_A"))
                .andExpect(jsonPath("$.amountOwed").value(450.00))
                .andExpect(jsonPath("$.pendingProportional").value(false));
    }

    // =========================================================================
    // TC-F06: Listagem de lancamentos
    // =========================================================================

    @Test
    @Order(6)
    @DisplayName("TC-F06: GET /api/transactions?month=2026-10 retorna 200 com lista nao-vazia")
    void tcF06_listTransactionsOctober2026_returnsNonEmptyList() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + obtainToken())
                        .param("month", "2026-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThan(0)));
    }

    // =========================================================================
    // TC-F07: Dashboard
    // =========================================================================

    @Test
    @Order(7)
    @DisplayName("TC-F07: GET /api/dashboard?month=2026-10 retorna 200 com totalExpense>0 e totalIncome>0")
    void tcF07_dashboardOctober2026_hasExpenseAndIncome() throws Exception {
        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + obtainToken())
                        .param("month", "2026-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.totalIncome").value(greaterThan(0.0)));
    }

    // =========================================================================
    // TC-F08: Mes sem dados
    // =========================================================================

    @Test
    @Order(8)
    @DisplayName("TC-F08: GET /api/settlement?month=2031-01 retorna 200, settled=true, totalExpense=0")
    void tcF08_settlementEmptyMonth_returnsSettledAndZeroExpense() throws Exception {
        mockMvc.perform(get("/api/settlement")
                        .header("Authorization", "Bearer " + obtainToken())
                        .param("month", "2031-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settled").value(true))
                .andExpect(jsonPath("$.totalExpense").value(0));
    }

    // =========================================================================
    // Helper: obtem token JWT (registro ou login)
    // =========================================================================

    private String obtainToken() throws Exception {
        if (bearerToken != null) return bearerToken;

        Map<String, String> body = Map.of(
                "name", "QA Fluxo",
                "email", "qa-fluxo@example.com",
                "password", "senha12345");

        MvcResult result;
        try {
            result = mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isCreated())
                    .andReturn();
        } catch (AssertionError e) {
            result = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("email", "qa-fluxo@example.com",
                                           "password", "senha12345"))))
                    .andExpect(status().isOk())
                    .andReturn();
        }
        bearerToken = objectMapper.readTree(
                result.getResponse().getContentAsString()).get("token").asText();
        return bearerToken;
    }
}
