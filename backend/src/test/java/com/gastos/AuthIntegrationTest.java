package com.gastos;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // GET /api/health
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("GET /api/health retorna 200 sem autenticação")
    void health_withoutToken_returns200() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    // -------------------------------------------------------------------------
    // Endpoint protegido sem token
    // -------------------------------------------------------------------------

    @Test
    @Order(2)
    @DisplayName("Endpoint protegido sem Bearer token retorna 401")
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // POST /api/auth/register — caminho feliz
    // -------------------------------------------------------------------------

    @Test
    @Order(3)
    @DisplayName("POST /api/auth/register com dados válidos retorna 201 e JWT")
    void register_withValidData_returns201AndJwt() throws Exception {
        Map<String, String> body = Map.of(
                "name", "Alice",
                "email", "alice@example.com",
                "password", "senha123"
        );

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
        assertThat(token.split("\\.")).hasSize(3);
    }

    // -------------------------------------------------------------------------
    // POST /api/auth/register — e-mail duplicado → 409
    // -------------------------------------------------------------------------

    @Test
    @Order(4)
    @DisplayName("POST /api/auth/register com e-mail duplicado retorna 409")
    void register_withDuplicateEmail_returns409() throws Exception {
        Map<String, String> first = Map.of(
                "name", "Bob",
                "email", "bob@example.com",
                "password", "senha123"
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        Map<String, String> duplicate = Map.of(
                "name", "Bob Duplicado",
                "email", "bob@example.com",
                "password", "outrasenha"
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("E-mail já cadastrado"));
    }

    // -------------------------------------------------------------------------
    // POST /api/auth/register — campos inválidos → 400
    // -------------------------------------------------------------------------

    @Test
    @Order(5)
    @DisplayName("POST /api/auth/register sem nome retorna 400 com mensagem pt-br")
    void register_withBlankName_returns400() throws Exception {
        Map<String, String> body = Map.of(
                "name", "",
                "email", "invalido@example.com",
                "password", "senha123"
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("Nome é obrigatório"));
    }

    @Test
    @Order(6)
    @DisplayName("POST /api/auth/register com e-mail inválido retorna 400 com mensagem pt-br")
    void register_withInvalidEmail_returns400() throws Exception {
        Map<String, String> body = Map.of(
                "name", "Carlos",
                "email", "nao-e-email",
                "password", "senha123"
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("E-mail inválido"));
    }

    @Test
    @Order(7)
    @DisplayName("POST /api/auth/register com senha curta retorna 400 com mensagem pt-br")
    void register_withShortPassword_returns400() throws Exception {
        Map<String, String> body = Map.of(
                "name", "Diana",
                "email", "diana@example.com",
                "password", "abc"
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").value("Senha deve ter no mínimo 8 caracteres"));
    }

    @Test
    @Order(8)
    @DisplayName("POST /api/auth/register com múltiplos campos inválidos retorna 400 com todos os erros")
    void register_withMultipleInvalidFields_returns400WithAllErrors() throws Exception {
        Map<String, String> body = Map.of(
                "name", "",
                "email", "nao-e-email",
                "password", "123"
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.password").exists());
    }

    // -------------------------------------------------------------------------
    // POST /api/auth/login — caminho feliz
    // -------------------------------------------------------------------------

    @Test
    @Order(9)
    @DisplayName("POST /api/auth/login com credenciais válidas retorna 200 e JWT")
    void login_withValidCredentials_returns200AndJwt() throws Exception {
        Map<String, String> registerBody = Map.of(
                "name", "Fernanda",
                "email", "fernanda@example.com",
                "password", "minhasenha"
        );
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerBody)));

        Map<String, String> loginBody = Map.of(
                "email", "fernanda@example.com",
                "password", "minhasenha"
        );
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.name").value("Fernanda"))
                .andExpect(jsonPath("$.email").value("fernanda@example.com"))
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
        assertThat(token.split("\\.")).hasSize(3);
    }

    // -------------------------------------------------------------------------
    // POST /api/auth/login — credenciais erradas → 401
    // -------------------------------------------------------------------------

    @Test
    @Order(10)
    @DisplayName("POST /api/auth/login com senha errada retorna 401")
    void login_withWrongPassword_returns401() throws Exception {
        Map<String, String> registerBody = Map.of(
                "name", "Gabriel",
                "email", "gabriel@example.com",
                "password", "senhaCorreta"
        );
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerBody)));

        Map<String, String> loginBody = Map.of(
                "email", "gabriel@example.com",
                "password", "senhaErrada"
        );
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(11)
    @DisplayName("POST /api/auth/login com e-mail inexistente retorna 401")
    void login_withNonExistentEmail_returns401() throws Exception {
        Map<String, String> loginBody = Map.of(
                "email", "naoexiste@example.com",
                "password", "qualquersenha"
        );
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // POST /api/auth/login — campos inválidos → 400
    // -------------------------------------------------------------------------

    @Test
    @Order(12)
    @DisplayName("POST /api/auth/login sem e-mail retorna 400 com mensagem pt-br")
    void login_withBlankEmail_returns400() throws Exception {
        Map<String, String> body = Map.of(
                "email", "",
                "password", "senha123"
        );
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("E-mail é obrigatório"));
    }

    @Test
    @Order(13)
    @DisplayName("POST /api/auth/login sem senha retorna 400 com mensagem pt-br")
    void login_withBlankPassword_returns400() throws Exception {
        Map<String, String> body = Map.of(
                "email", "alguem@example.com",
                "password", ""
        );
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").value("Senha é obrigatória"));
    }

    // -------------------------------------------------------------------------
    // Endpoint protegido COM token válido — não deve retornar 401
    // -------------------------------------------------------------------------

    @Test
    @Order(14)
    @DisplayName("Endpoint protegido com Bearer token válido retorna 404 (não 401)")
    void protectedEndpoint_withValidToken_returns404NotUnauthorized() throws Exception {
        Map<String, String> registerBody = Map.of(
                "name", "Helena",
                "email", "helena@example.com",
                "password", "senhasegura"
        );
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isCreated())
                .andReturn();

        String token = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("token").asText();

        mockMvc.perform(get("/api/recurso-inexistente")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
