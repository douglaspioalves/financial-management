package com.gastos;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração dos endpoints de acerto de contas (Settlement).
 *
 * Contrato esperado (quando SettlementController for mergeado):
 *   GET /api/settlement?month=yyyy-MM-01
 *
 * Testes sem @Disabled são executados agora (apenas autenticação e rota básica).
 * Testes marcados com @Disabled dependem do merge de feature/s06-backend-settlement.
 * Remover @Disabled após o merge e antes do release da fatia 6.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SettlementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String bearerToken;

    // -------------------------------------------------------------------------
    // Helper: obtém Bearer token (registra ou faz login)
    // -------------------------------------------------------------------------

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
    // Testes que rodam agora (independentes do SettlementController)
    // =========================================================================

    /**
     * Garante que o endpoint de settlement exige autenticação.
     * Deve retornar 401 quando não há token JWT.
     *
     * NOTA: Este teste só passará após o merge de feature/s06-backend-settlement,
     * pois o SettlementController não existe ainda no master.
     * O endpoint pode retornar 401 (Spring Security bloqueia) ou 404 (rota não mapeada).
     * Aceitamos ambos aqui para que o teste seja útil tanto antes quanto após o merge.
     */
    @Test
    @Order(1)
    @DisplayName("GET /api/settlement sem token → 401 (autenticação obrigatória)")
    void getSettlement_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/settlement")
                        .param("month", "2026-05-01"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // Testes que dependem do merge de feature/s06-backend-settlement
    // =========================================================================

    /**
     * Verifica que GET /api/settlement com JWT válido e mês sem dados retorna 200
     * com settled=true e totalExpense=0.
     *
     * Habilitar após: merge de feature/s06-backend-settlement
     */
    @Test
    @Order(2)
    @Disabled("Aguardando merge de feature/s06-backend-settlement no master")
    @DisplayName("GET /api/settlement com token e sem dados → 200, settled=true, totalExpense=0")
    void getSettlement_withTokenAndNoData_returns200Settled() throws Exception {
        mockMvc.perform(get("/api/settlement")
                        .header("Authorization", "Bearer " + obtainToken())
                        .param("month", "2026-05-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settled").value(true))
                .andExpect(jsonPath("$.totalExpense").value(0))
                .andExpect(jsonPath("$.pendingProportional").value(false))
                .andExpect(jsonPath("$.amountOwed").doesNotExist());
    }

    /**
     * Verifica que GET /api/settlement sem parâmetro month retorna 400.
     *
     * Habilitar após: merge de feature/s06-backend-settlement
     */
    @Test
    @Order(3)
    @Disabled("Aguardando merge de feature/s06-backend-settlement no master")
    @DisplayName("GET /api/settlement sem parâmetro month → 400")
    void getSettlement_withoutMonthParam_returns400() throws Exception {
        mockMvc.perform(get("/api/settlement")
                        .header("Authorization", "Bearer " + obtainToken()))
                .andExpect(status().isBadRequest());
    }

    /**
     * Fluxo completo: cria receitas individuais, despesa FIFTY_FIFTY,
     * e verifica que o acerto retorna o devedor e valor corretos.
     *
     * Habilitar após: merge de feature/s06-backend-settlement
     */
    @Test
    @Order(4)
    @Disabled("Aguardando merge de feature/s06-backend-settlement no master")
    @DisplayName("GET /api/settlement — fluxo completo FIFTY_FIFTY → PersonB deve R$500 a PersonA")
    void getSettlement_fullFlow_fiftyFifty_personBOwes500() throws Exception {
        // Este teste será implementado quando o SettlementController estiver no master.
        // Passos:
        //   1. POST /api/transactions — despesa R$1000 FIFTY_FIFTY paga por PersonA
        //   2. GET /api/settlement?month=2026-05-01
        //   3. Verificar: debtor=PERSON_B, creditor=PERSON_A, amountOwed=500.00

        // Placeholder — remover esta linha ao implementar
        throw new UnsupportedOperationException(
                "Implementar após merge de feature/s06-backend-settlement");
    }

    /**
     * Verifica que despesas PROPORTIONAL sem receitas individuais cadastradas
     * retornam pendingProportional=true e a mensagem de orientação.
     *
     * Habilitar após: merge de feature/s06-backend-settlement
     */
    @Test
    @Order(5)
    @Disabled("Aguardando merge de feature/s06-backend-settlement no master")
    @DisplayName("GET /api/settlement — despesa PROPORTIONAL sem receitas → pendingProportional=true")
    void getSettlement_proportionalWithoutIncome_returnsPending() throws Exception {
        // Placeholder — implementar após merge
        throw new UnsupportedOperationException(
                "Implementar após merge de feature/s06-backend-settlement");
    }

    /**
     * Verifica que parcelas de compras parceladas entram no acerto pelo mês
     * da parcela (referenceMonth), não pelo mês da compra.
     *
     * Habilitar após: merge de feature/s06-backend-settlement
     */
    @Test
    @Order(6)
    @Disabled("Aguardando merge de feature/s06-backend-settlement no master")
    @DisplayName("GET /api/settlement — parcela de cartão entra pelo referenceMonth, não pela data da compra")
    void getSettlement_installmentEntersByReferenceMonth() throws Exception {
        // Placeholder — implementar após merge
        throw new UnsupportedOperationException(
                "Implementar após merge de feature/s06-backend-settlement");
    }
}
