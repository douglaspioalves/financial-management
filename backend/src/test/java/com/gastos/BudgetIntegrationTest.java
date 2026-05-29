package com.gastos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gastos.domain.Category;
import com.gastos.domain.CategoryType;
import com.gastos.domain.Person;
import com.gastos.repository.CategoryRepository;
import com.gastos.repository.PersonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integracao dos endpoints de orcamento (Budget).
 *
 * ATENCAO: todos os testes estao anotados com @Disabled pois o BudgetController
 * ainda nao foi mergeado no master. Remover @Disabled apos o merge de
 * feature/s05-backend-budget.
 *
 * Contrato esperado:
 *   GET  /api/budgets?month=yyyy-MM-01           -> lista de orcamentos do mes
 *   POST /api/budgets                            -> cria orcamento (201)
 *   PUT  /api/budgets/{id}                       -> atualiza (200) ou conflito de versao (409)
 *   DELETE /api/budgets/{id}                     -> exclui (204)
 *
 * Request POST/PUT:
 *   { "categoryId": "uuid", "month": "yyyy-MM-dd", "limitAmount": <number>, "version": <long> }
 *
 * Response GET/POST/PUT:
 *   {
 *     "id": "uuid",
 *     "categoryId": "uuid",
 *     "categoryName": "string",
 *     "month": "yyyy-MM-dd",
 *     "limitAmount": <number>,
 *     "spentAmount": <number>,
 *     "percentage": <number>,
 *     "status": "OK" | "WARNING" | "EXCEEDED",
 *     "version": <long>
 *   }
 *
 * Regras de negocio:
 *   - month deve ser o dia 1 do mes (ex.: "2026-05-01"). Qualquer outro dia -> 400.
 *   - Cada categoria so pode ter um orcamento por mes (unicidade). Segundo POST -> 409.
 *   - spentAmount = soma das despesas (incluindo parcelas) da categoria no mes.
 *   - percentage = (spentAmount / limitAmount) * 100.
 *   - status: OK se percentage <= 70, WARNING se > 70 e <= 100, EXCEEDED se > 100.
 *   - PUT com version diferente da atual -> 409 (optimistic locking).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled("Aguardando merge de feature/s05-backend-budget")
class BudgetIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private static String bearerToken;
    private static UUID   testPersonId;
    private static UUID   expenseCategoryId;
    private static String createdBudgetId;
    private static Long   createdBudgetVersion;

    // -------------------------------------------------------------------------
    // Setup: garante que person e categoria existem
    // -------------------------------------------------------------------------

    @BeforeEach
    void ensureFixtures() {
        if (testPersonId == null) {
            Person person = personRepository.save(
                    Person.builder()
                            .name("QA Budget Pessoa")
                            .color("#4a7fc4")
                            .version(0L)
                            .build());
            testPersonId = person.getId();
        }
        if (expenseCategoryId == null) {
            Category cat = categoryRepository.save(
                    Category.builder()
                            .name("Alimentacao QA Budget")
                            .type(CategoryType.EXPENSE)
                            .color("#e88a74")
                            .version(0L)
                            .build());
            expenseCategoryId = cat.getId();
        }
    }

    // -------------------------------------------------------------------------
    // Helper: obtem Bearer token
    // -------------------------------------------------------------------------

    private String obtainToken() throws Exception {
        if (bearerToken != null) return bearerToken;

        Map<String, String> register = Map.of(
                "name", "QA Budget",
                "email", "qa-budget@example.com",
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
                                    Map.of("email", "qa-budget@example.com", "password", "senha12345"))))
                    .andExpect(status().isOk())
                    .andReturn();
        }
        bearerToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
        return bearerToken;
    }

    // -------------------------------------------------------------------------
    // Helper: cria uma despesa via API
    // -------------------------------------------------------------------------

    private void criaDespesa(String amount, String date, UUID categoryId, UUID personId) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("type", "EXPENSE");
        body.put("amount", new BigDecimal(amount));
        body.put("date", date);
        body.put("categoryId", categoryId.toString());
        body.put("paidByPersonId", personId.toString());
        body.put("paymentMethod", "CASH");
        body.put("splitRule", "FIFTY_FIFTY");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + obtainToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    // -------------------------------------------------------------------------
    // TC-B1: GET /api/budgets sem JWT -> 401
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("TC-B1: GET /api/budgets sem token retorna 401")
    void getBudgets_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/budgets?month=2026-05-01"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // TC-B2: POST /api/budgets com dados validos -> 201
    // -------------------------------------------------------------------------

    @Test
    @Order(2)
    @DisplayName("TC-B2: POST /api/budgets com dados validos retorna 201 e orcamento criado")
    void createBudget_withValidData_returns201() throws Exception {
        Map<String, Object> body = Map.of(
                "categoryId", expenseCategoryId.toString(),
                "month", "2026-05-01",
                "limitAmount", new BigDecimal("500.00"));

        MvcResult result = mockMvc.perform(post("/api/budgets")
                        .header("Authorization", "Bearer " + obtainToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.categoryId").value(expenseCategoryId.toString()))
                .andExpect(jsonPath("$.limitAmount").value(500.00))
                .andExpect(jsonPath("$.spentAmount").value(0.00))
                .andExpect(jsonPath("$.percentage").value(0.00))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.version").isNumber())
                .andReturn();

        var node = objectMapper.readTree(result.getResponse().getContentAsString());
        createdBudgetId = node.get("id").asText();
        createdBudgetVersion = node.get("version").asLong();
        assertThat(createdBudgetId).isNotBlank();
    }

    // -------------------------------------------------------------------------
    // TC-B3: POST /api/budgets com month que nao e dia 1 -> 400 com mensagem pt-br
    // -------------------------------------------------------------------------

    @Test
    @Order(3)
    @DisplayName("TC-B3: POST /api/budgets com month=2026-05-15 (nao e dia 1) retorna 400")
    void createBudget_withNonFirstDayMonth_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "categoryId", expenseCategoryId.toString(),
                "month", "2026-05-15",
                "limitAmount", new BigDecimal("500.00"));

        mockMvc.perform(post("/api/budgets")
                        .header("Authorization", "Bearer " + obtainToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").isNotEmpty());
    }

    // -------------------------------------------------------------------------
    // TC-B4: POST /api/budgets duas vezes para mesma categoria+mes -> 409
    // -------------------------------------------------------------------------

    @Test
    @Order(4)
    @DisplayName("TC-B4: POST /api/budgets duas vezes para mesma categoria e mes retorna 409")
    void createBudget_duplicateCategoryMonth_returns409() throws Exception {
        // Cria categoria exclusiva para este teste (evitar conflito com TC-B2)
        Category catDup = categoryRepository.save(
                Category.builder()
                        .name("Transporte QA Budget Dup")
                        .type(CategoryType.EXPENSE)
                        .color("#aaaaaa")
                        .version(0L)
                        .build());

        Map<String, Object> body = Map.of(
                "categoryId", catDup.getId().toString(),
                "month", "2026-06-01",
                "limitAmount", new BigDecimal("300.00"));

        // Primeiro POST: 201
        mockMvc.perform(post("/api/budgets")
                        .header("Authorization", "Bearer " + obtainToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        // Segundo POST para mesmo categoria+mes: 409
        mockMvc.perform(post("/api/budgets")
                        .header("Authorization", "Bearer " + obtainToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").isNotEmpty());
    }

    // -------------------------------------------------------------------------
    // TC-B5: GET /api/budgets com despesa lancada -> spentAmount e percentage corretos
    //
    // Orcamento: R$ 200.00 limite para alimentacao em marco/2099
    // Despesa: R$ 100.00 na mesma categoria e mes
    // -> spentAmount = 100.00, percentage = 50.00, status = OK
    // -------------------------------------------------------------------------

    @Test
    @Order(5)
    @DisplayName("TC-B5: GET /api/budgets reflete spentAmount e percentage corretos apos lancamento de despesa")
    void getBudgets_afterExpense_returnsCorrectSpentAmountAndPercentage() throws Exception {
        // Categoria exclusiva para este teste
        Category catTest = categoryRepository.save(
                Category.builder()
                        .name("Lazer QA Budget TC5")
                        .type(CategoryType.EXPENSE)
                        .color("#bbbbbb")
                        .version(0L)
                        .build());

        // Cria orcamento de R$ 200.00 para marco/2099
        Map<String, Object> budgetBody = Map.of(
                "categoryId", catTest.getId().toString(),
                "month", "2099-03-01",
                "limitAmount", new BigDecimal("200.00"));
        mockMvc.perform(post("/api/budgets")
                        .header("Authorization", "Bearer " + obtainToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(budgetBody)))
                .andExpect(status().isCreated());

        // Lanca despesa de R$ 100.00 na mesma categoria e mes
        criaDespesa("100.00", "2099-03-15", catTest.getId(), testPersonId);

        // GET /api/budgets?month=2099-03-01 deve retornar spentAmount=100 e percentage=50
        mockMvc.perform(get("/api/budgets?month=2099-03-01")
                        .header("Authorization", "Bearer " + obtainToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].spentAmount").value(100.00))
                .andExpect(jsonPath("$[0].percentage").value(50.00))
                .andExpect(jsonPath("$[0].status").value("OK"));
    }

    // -------------------------------------------------------------------------
    // TC-B6: PUT /api/budgets/{id} com version correta -> 200
    // -------------------------------------------------------------------------

    @Test
    @Order(6)
    @DisplayName("TC-B6: PUT /api/budgets/{id} com version correta atualiza e retorna 200")
    void updateBudget_withCorrectVersion_returns200() throws Exception {
        assertThat(createdBudgetId).isNotNull()
                .as("TC-B2 deve ter rodado antes para criar o orcamento");

        Map<String, Object> updateBody = Map.of(
                "categoryId", expenseCategoryId.toString(),
                "month", "2026-05-01",
                "limitAmount", new BigDecimal("750.00"),
                "version", createdBudgetVersion);

        MvcResult result = mockMvc.perform(put("/api/budgets/" + createdBudgetId)
                        .header("Authorization", "Bearer " + obtainToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limitAmount").value(750.00))
                .andReturn();

        // Salva nova versao para TC-B7
        createdBudgetVersion = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("version").asLong();
    }

    // -------------------------------------------------------------------------
    // TC-B7: PUT /api/budgets/{id} com version errada -> 409 (optimistic locking)
    // -------------------------------------------------------------------------

    @Test
    @Order(7)
    @DisplayName("TC-B7: PUT /api/budgets/{id} com version errada retorna 409 (conflito de concorrencia)")
    void updateBudget_withStaleVersion_returns409() throws Exception {
        assertThat(createdBudgetId).isNotNull()
                .as("TC-B2 deve ter rodado antes para criar o orcamento");

        // Usa version 0 que ja nao e a atual (TC-B6 incrementou)
        long staleVersion = 0L;
        Map<String, Object> updateBody = Map.of(
                "categoryId", expenseCategoryId.toString(),
                "month", "2026-05-01",
                "limitAmount", new BigDecimal("900.00"),
                "version", staleVersion);

        mockMvc.perform(put("/api/budgets/" + createdBudgetId)
                        .header("Authorization", "Bearer " + obtainToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").isNotEmpty());
    }

    // -------------------------------------------------------------------------
    // TC-B8: DELETE /api/budgets/{id} -> 204, nao existe mais
    // -------------------------------------------------------------------------

    @Test
    @Order(8)
    @DisplayName("TC-B8: DELETE /api/budgets/{id} retorna 204 e orcamento e removido")
    void deleteBudget_returns204AndBudgetNoLongerExists() throws Exception {
        // Cria orcamento especifico para este teste
        Category catDelete = categoryRepository.save(
                Category.builder()
                        .name("Saude QA Budget Delete")
                        .type(CategoryType.EXPENSE)
                        .color("#cccccc")
                        .version(0L)
                        .build());

        Map<String, Object> body = Map.of(
                "categoryId", catDelete.getId().toString(),
                "month", "2026-07-01",
                "limitAmount", new BigDecimal("1000.00"));

        MvcResult createResult = mockMvc.perform(post("/api/budgets")
                        .header("Authorization", "Bearer " + obtainToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();

        String budgetId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asText();

        // DELETE -> 204
        mockMvc.perform(delete("/api/budgets/" + budgetId)
                        .header("Authorization", "Bearer " + obtainToken()))
                .andExpect(status().isNoContent());

        // Verifica que o orcamento nao aparece mais no mes
        mockMvc.perform(get("/api/budgets?month=2026-07-01")
                        .header("Authorization", "Bearer " + obtainToken()))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String content = result.getResponse().getContentAsString();
                    assertThat(content).doesNotContain(budgetId);
                });
    }

    // -------------------------------------------------------------------------
    // TC-B9: GET /api/budgets com despesa parcelada -> apenas parcela do mes correto
    //        entra em spentAmount
    //
    // Orcamento: R$ 500.00 em agosto/2099
    // Compra parcelada: R$ 300, 3x em agosto/2099 (closingDay=20, data=10/08)
    //   -> 1a parcela = agosto (R$ 100), 2a = setembro (R$ 100), 3a = outubro (R$ 100)
    // spentAmount de agosto deve ser R$ 100 (somente 1a parcela)
    // -------------------------------------------------------------------------

    @Test
    @Order(9)
    @DisplayName("TC-B9: Budget — spentAmount de parcela parcelada considera apenas parcela do mes correto")
    void getBudgets_withInstallmentPurchase_spentAmountOnlyCountsCurrentMonthInstallment()
            throws Exception {
        // Categoria exclusiva
        Category catInstallment = categoryRepository.save(
                Category.builder()
                        .name("Eletronicos QA Budget TC9")
                        .type(CategoryType.EXPENSE)
                        .color("#dddddd")
                        .version(0L)
                        .build());

        // Cria cartao com closingDay=20
        Map<String, Object> cardBody = Map.of(
                "name", "Cartao Budget TC9",
                "ownerPersonId", testPersonId.toString(),
                "closingDay", 20,
                "dueDay", 27);
        MvcResult cardResult = mockMvc.perform(post("/api/cards")
                        .header("Authorization", "Bearer " + obtainToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardBody)))
                .andExpect(status().isCreated())
                .andReturn();
        String cardId = objectMapper.readTree(cardResult.getResponse().getContentAsString())
                .get("id").asText();

        // Cria orcamento de R$ 500.00 para agosto/2099
        Map<String, Object> budgetBody = Map.of(
                "categoryId", catInstallment.getId().toString(),
                "month", "2099-08-01",
                "limitAmount", new BigDecimal("500.00"));
        mockMvc.perform(post("/api/budgets")
                        .header("Authorization", "Bearer " + obtainToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(budgetBody)))
                .andExpect(status().isCreated());

        // Compra parcelada: R$ 300, 3x, dia 10/08 (antes do fechamento 20)
        // -> 1a parcela = agosto, 2a = setembro, 3a = outubro
        Map<String, Object> txBody = new HashMap<>();
        txBody.put("type", "EXPENSE");
        txBody.put("amount", new BigDecimal("300.00"));
        txBody.put("date", "2099-08-10");
        txBody.put("categoryId", catInstallment.getId().toString());
        txBody.put("paidByPersonId", testPersonId.toString());
        txBody.put("paymentMethod", "CREDIT");
        txBody.put("cardId", cardId);
        txBody.put("splitRule", "FIFTY_FIFTY");
        txBody.put("installmentsTotal", 3);
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + obtainToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(txBody)))
                .andExpect(status().isCreated());

        // spentAmount de agosto deve ser R$ 100.00 (apenas 1a parcela)
        mockMvc.perform(get("/api/budgets?month=2099-08-01")
                        .header("Authorization", "Bearer " + obtainToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].spentAmount").value(100.00))
                .andExpect(jsonPath("$[0].percentage").value(20.00));
    }
}
