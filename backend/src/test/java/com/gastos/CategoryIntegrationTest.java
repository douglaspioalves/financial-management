package com.gastos;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class CategoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Token compartilhado entre os testes (obtido no primeiro teste autenticado)
    private static String bearerToken;

    // ID da categoria criada, reutilizado em testes subsequentes
    private static String createdCategoryId;
    private static Long createdCategoryVersion;

    // -------------------------------------------------------------------------
    // Helper: registra usuário e extrai Bearer token
    // -------------------------------------------------------------------------

    private String obtainToken() throws Exception {
        if (bearerToken != null) {
            return bearerToken;
        }

        Map<String, String> registerBody = Map.of(
                "name", "QA Tester",
                "email", "qa-categories@example.com",
                "password", "senha12345"
        );

        MvcResult result;
        try {
            result = mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerBody)))
                    .andExpect(status().isCreated())
                    .andReturn();
        } catch (AssertionError e) {
            // Usuário já existe — faz login
            Map<String, String> loginBody = Map.of(
                    "email", "qa-categories@example.com",
                    "password", "senha12345"
            );
            result = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginBody)))
                    .andExpect(status().isOk())
                    .andReturn();
        }

        bearerToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
        return bearerToken;
    }

    // -------------------------------------------------------------------------
    // GET /api/categories — sem token → 401
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("GET /api/categories sem token retorna 401")
    void listCategories_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // GET /api/categories — com token → 200
    // -------------------------------------------------------------------------

    @Test
    @Order(2)
    @DisplayName("GET /api/categories com token válido retorna 200 e lista")
    void listCategories_withValidToken_returns200() throws Exception {
        String token = obtainToken();

        mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // POST /api/categories — dados válidos → 201
    // -------------------------------------------------------------------------

    @Test
    @Order(3)
    @DisplayName("POST /api/categories com dados válidos retorna 201 com body correto")
    void createCategory_withValidData_returns201WithBody() throws Exception {
        String token = obtainToken();

        Map<String, String> body = Map.of(
                "name", "Alimentação",
                "type", "EXPENSE",
                "color", "#FF5733"
        );

        MvcResult result = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Alimentação"))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.color").value("#FF5733"))
                .andExpect(jsonPath("$.version").isNumber())
                .andReturn();

        var responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        createdCategoryId = responseNode.get("id").asText();
        createdCategoryVersion = responseNode.get("version").asLong();

        assertThat(createdCategoryId).isNotBlank()
                .as("ID da categoria criada não deve ser vazio");
    }

    // -------------------------------------------------------------------------
    // POST /api/categories — sem nome → 400
    // -------------------------------------------------------------------------

    @Test
    @Order(4)
    @DisplayName("POST /api/categories sem nome retorna 400 com mensagem pt-br")
    void createCategory_withoutName_returns400WithPtBrMessage() throws Exception {
        String token = obtainToken();

        Map<String, Object> body = Map.of(
                "type", "EXPENSE",
                "color", "#FF5733"
        );

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("Nome da categoria é obrigatório."));
    }

    // -------------------------------------------------------------------------
    // POST /api/categories — cor inválida → 400
    // -------------------------------------------------------------------------

    @Test
    @Order(5)
    @DisplayName("POST /api/categories com cor inválida retorna 400 com mensagem pt-br")
    void createCategory_withInvalidColor_returns400WithPtBrMessage() throws Exception {
        String token = obtainToken();

        Map<String, String> body = Map.of(
                "name", "Transporte",
                "type", "INCOME",
                "color", "vermelho"
        );

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.color").value("Cor deve estar no formato hexadecimal #RRGGBB."));
    }

    // -------------------------------------------------------------------------
    // POST /api/categories — nome duplicado → 409
    // -------------------------------------------------------------------------

    @Test
    @Order(6)
    @DisplayName("POST /api/categories com nome duplicado retorna 409")
    void createCategory_withDuplicateName_returns409() throws Exception {
        String token = obtainToken();

        // Tenta criar de novo com o mesmo nome da categoria criada no teste 3
        Map<String, String> body = Map.of(
                "name", "Alimentação",
                "type", "BOTH",
                "color", "#00FF00"
        );

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Já existe uma categoria ativa com este nome."));
    }

    // -------------------------------------------------------------------------
    // PUT /api/categories/{id} — sucesso → 200
    // -------------------------------------------------------------------------

    @Test
    @Order(7)
    @DisplayName("PUT /api/categories/{id} com dados válidos retorna 200")
    void updateCategory_withValidData_returns200() throws Exception {
        String token = obtainToken();

        assertThat(createdCategoryId).isNotNull()
                .as("O ID da categoria deve ter sido criado no teste anterior (ordem 3)");

        Map<String, Object> body = Map.of(
                "name", "Alimentação e Bebidas",
                "color", "#AA3311",
                "version", createdCategoryVersion
        );

        mockMvc.perform(put("/api/categories/" + createdCategoryId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alimentação e Bebidas"))
                .andExpect(jsonPath("$.color").value("#AA3311"));
    }

    // -------------------------------------------------------------------------
    // PUT /api/categories/{id} — não encontrado → 404
    // -------------------------------------------------------------------------

    @Test
    @Order(8)
    @DisplayName("PUT /api/categories/{id} com ID inexistente retorna 404")
    void updateCategory_withUnknownId_returns404() throws Exception {
        String token = obtainToken();

        Map<String, Object> body = Map.of(
                "name", "Qualquer",
                "color", "#AABBCC",
                "version", 0L
        );

        String unknownId = UUID.randomUUID().toString();

        mockMvc.perform(put("/api/categories/" + unknownId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Categoria não encontrada."));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/categories/{id} — sucesso → 204
    // -------------------------------------------------------------------------

    @Test
    @Order(9)
    @DisplayName("DELETE /api/categories/{id} com ID válido retorna 204")
    void deleteCategory_withValidId_returns204() throws Exception {
        String token = obtainToken();

        assertThat(createdCategoryId).isNotNull()
                .as("O ID da categoria deve ter sido criado no teste anterior (ordem 3)");

        mockMvc.perform(delete("/api/categories/" + createdCategoryId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    // -------------------------------------------------------------------------
    // DELETE /api/categories/{id} — não encontrado → 404
    // -------------------------------------------------------------------------

    @Test
    @Order(10)
    @DisplayName("DELETE /api/categories/{id} com ID inexistente retorna 404")
    void deleteCategory_withUnknownId_returns404() throws Exception {
        String token = obtainToken();

        String unknownId = UUID.randomUUID().toString();

        mockMvc.perform(delete("/api/categories/" + unknownId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Categoria não encontrada."));
    }

    // -------------------------------------------------------------------------
    // GET /api/categories — após DELETE, categoria não aparece na lista
    // -------------------------------------------------------------------------

    @Test
    @Order(11)
    @DisplayName("GET /api/categories após DELETE não retorna a categoria deletada")
    void listCategories_afterDelete_doesNotReturnDeletedCategory() throws Exception {
        String token = obtainToken();

        assertThat(createdCategoryId).isNotNull()
                .as("O ID da categoria deletada deve estar disponível dos testes anteriores");

        String responseBody = mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody).doesNotContain(createdCategoryId)
                .as("A categoria deletada (soft delete) não deve aparecer na listagem");
    }
}
