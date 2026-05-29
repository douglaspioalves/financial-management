package com.gastos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gastos.domain.Category;
import com.gastos.domain.CategoryType;
import com.gastos.domain.Person;
import com.gastos.repository.CardRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integracao do endpoint GET /api/dashboard.
 *
 * ATENCAO: todos os testes estao anotados com @Disabled pois o DashboardController
 * ainda nao foi mergeado no master. Remover @Disabled apos o merge de
 * feature/s05-backend-dashboard.
 *
 * Contrato esperado:
 *   GET /api/dashboard?month=yyyy-MM
 *   Response 200:
 *   {
 *     "totalIncome":   number,
 *     "totalExpense":  number,
 *     "balance":       number,
 *     "byCategory": [ { "categoryId", "categoryName", "color", "total", "percentage" } ],
 *     "recentTransactions": [ ... ultimos lancamentos ],
 *     "previousMonth": { "totalIncome", "totalExpense", "balance" }
 *   }
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled("Aguardando merge de feature/s05-backend-dashboard")
class DashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CardRepository cardRepository;

    private static String bearerToken;
    private static UUID   testPersonId;
    private static UUID   expenseCategoryId;
    private static UUID   incomeCategoryId;

    @BeforeEach
    void ensureFixtures() {
        if (testPersonId == null) {
            Person person = personRepository.save(
                    Person.builder().name("QA Dashboard Pessoa").color("#4a7fc4").version(0L).build());
            testPersonId = person.getId();
        }
        if (expenseCategoryId == null) {
            Category cat = categoryRepository.save(
                    Category.builder().name("Alimentacao QA Dashboard").type(CategoryType.EXPENSE)
                            .color("#e88a74").version(0L).build());
            expenseCategoryId = cat.getId();
        }
        if (incomeCategoryId == null) {
            Category cat = categoryRepository.save(
                    Category.builder().name("Salario QA Dashboard").type(CategoryType.INCOME)
                            .color("#7ec8a4").version(0L).build());
            incomeCategoryId = cat.getId();
        }
    }

    private String obtainToken() throws Exception {
        if (bearerToken != null) return bearerToken;
        Map<String, String> register = Map.of("name", "QA Dashboard",
                "email", "qa-dashboard@example.com", "password", "senha12345");
        MvcResult result;
        try {
            result = mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(register)))
                    .andExpect(status().isCreated()).andReturn();
        } catch (AssertionError e) {
            result = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("email", "qa-dashboard@example.com", "password", "senha12345"))))
                    .andExpect(status().isOk()).andReturn();
        }
        bearerToken = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
        return bearerToken;
    }

    private void criaTransacao(String type, String amount, String date,
                                UUID categoryId, UUID personId,
                                String paymentMethod, String cardId,
                                String splitRule, Integer installmentsTotal) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("type", type);
        body.put("amount", new BigDecimal(amount));
        body.put("date", date);
        body.put("categoryId", categoryId.toString());
        body.put("paidByPersonId", personId.toString());
        body.put("paymentMethod", paymentMethod);
        body.put("splitRule", splitRule);
        if (cardId != null) body.put("cardId", cardId);
        if (installmentsTotal != null) body.put("installmentsTotal", installmentsTotal);
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + obtainToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    // TC-D1: sem JWT -> 401
    @Test @Order(1)
    @DisplayName("TC-D1: GET /api/dashboard sem token retorna 401")
    void getDashboard_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/dashboard?month=2026-05"))
                .andExpect(status().isUnauthorized());
    }

    // TC-D2: mes sem dados -> zeros
    @Test @Order(2)
    @DisplayName("TC-D2: GET /api/dashboard para mes sem dados retorna totais zero")
    void getDashboard_monthWithNoData_returnsZeros() throws Exception {
        mockMvc.perform(get("/api/dashboard?month=2099-12")
                        .header("Authorization", "Bearer " + obtainToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(0))
                .andExpect(jsonPath("$.totalExpense").value(0))
                .andExpect(jsonPath("$.balance").value(0));
    }

    // TC-D3: 1 receita (5000) + 1 despesa (1000) -> totais corretos
    @Test @Order(3)
    @DisplayName("TC-D3: Dashboard com 1 receita (R$5000) e 1 despesa (R$1000) retorna totais corretos")
    void getDashboard_withIncomeAndExpense_returnsCorrectTotals() throws Exception {
        criaTransacao("INCOME", "5000.00", "2026-05-05",
                incomeCategoryId, testPersonId, "TRANSFER", null, "PERSON_A", null);
        criaTransacao("EXPENSE", "1000.00", "2026-05-05",
                expenseCategoryId, testPersonId, "CASH", null, "FIFTY_FIFTY", null);
        mockMvc.perform(get("/api/dashboard?month=2026-05")
                        .header("Authorization", "Bearer " + obtainToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(5000.00))
                .andExpect(jsonPath("$.totalExpense").value(1000.00))
                .andExpect(jsonPath("$.balance").value(4000.00));
    }

    // TC-D4: despesa parcelada (R$300, 3x, closingDay=15, data=10/05)
    //        1a parcela em maio = R$100 -> dashboard de maio mostra 100, nao 300
    @Test @Order(4)
    @DisplayName("TC-D4: Compra parcelada — dashboard mostra apenas parcela do mes correto no totalExpense")
    void getDashboard_installmentPurchase_onlyCurrentMonthInstallmentInExpenseTotal() throws Exception {
        Map<String, Object> cardBody = Map.of("name", "Cartao Dashboard TC4",
                "ownerPersonId", testPersonId.toString(), "closingDay", 15, "dueDay", 22);
        MvcResult cardResult = mockMvc.perform(post("/api/cards")
                        .header("Authorization", "Bearer " + obtainToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardBody)))
                .andExpect(status().isCreated()).andReturn();
        String cardId = objectMapper.readTree(cardResult.getResponse().getContentAsString()).get("id").asText();

        // dia 10/05 < closingDay 15 -> 1a parcela = maio, 2a = junho, 3a = julho
        criaTransacao("EXPENSE", "300.00", "2026-05-10",
                expenseCategoryId, testPersonId, "CREDIT", cardId, "FIFTY_FIFTY", 3);

        mockMvc.perform(get("/api/dashboard?month=2026-05")
                        .header("Authorization", "Bearer " + obtainToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense").value(100.00));

        mockMvc.perform(get("/api/dashboard?month=2026-06")
                        .header("Authorization", "Bearer " + obtainToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense").value(100.00));
    }

    // TC-D5: previousMonth preenchido com dados do mes anterior
    @Test @Order(5)
    @DisplayName("TC-D5: Dashboard de maio inclui dados do mes anterior (abril) em previousMonth")
    void getDashboard_compareWithPreviousMonth_previousMonthFilledCorrectly() throws Exception {
        criaTransacao("EXPENSE", "800.00", "2026-04-10",
                expenseCategoryId, testPersonId, "CASH", null, "FIFTY_FIFTY", null);
        criaTransacao("EXPENSE", "600.00", "2026-05-10",
                expenseCategoryId, testPersonId, "CASH", null, "FIFTY_FIFTY", null);
        mockMvc.perform(get("/api/dashboard?month=2026-05")
                        .header("Authorization", "Bearer " + obtainToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense").value(600.00))
                .andExpect(jsonPath("$.previousMonth").exists())
                .andExpect(jsonPath("$.previousMonth.totalExpense").value(800.00));
    }

    // TC-D6: month ausente -> 400
    @Test @Order(6)
    @DisplayName("TC-D6: GET /api/dashboard sem parametro month retorna 400")
    void getDashboard_missingMonthParam_returns400() throws Exception {
        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + obtainToken()))
                .andExpect(status().isBadRequest());
    }

    // TC-D7: month formato invalido -> 400
    @Test @Order(7)
    @DisplayName("TC-D7: GET /api/dashboard com month em formato invalido retorna 400")
    void getDashboard_invalidMonthFormat_returns400() throws Exception {
        mockMvc.perform(get("/api/dashboard?month=05-2026")
                        .header("Authorization", "Bearer " + obtainToken()))
                .andExpect(status().isBadRequest());
    }

    // TC-D8: byCategory com percentual
    @Test @Order(8)
    @DisplayName("TC-D8: Dashboard agrupa despesas por categoria e expoe percentual")
    void getDashboard_expensesByCategory_groupedWithPercentage() throws Exception {
        criaTransacao("EXPENSE", "400.00", "2099-06-01",
                expenseCategoryId, testPersonId, "CASH", null, "FIFTY_FIFTY", null);
        criaTransacao("EXPENSE", "600.00", "2099-06-02",
                expenseCategoryId, testPersonId, "CASH", null, "FIFTY_FIFTY", null);
        mockMvc.perform(get("/api/dashboard?month=2099-06")
                        .header("Authorization", "Bearer " + obtainToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense").value(1000.00))
                .andExpect(jsonPath("$.byCategory").isArray())
                .andExpect(jsonPath("$.byCategory[0].categoryId").isNotEmpty())
                .andExpect(jsonPath("$.byCategory[0].total").value(1000.00))
                .andExpect(jsonPath("$.byCategory[0].percentage").value(100.0));
    }
}
