package br.com.documentshield;

import br.com.documentshield.repository.IntegracaoApiRepository;
import br.com.documentshield.security.ApiKeyHasher;
import com.fasterxml.jackson.databind.*;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:admin-api-key-test;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class AdminApiKeyIntegrationTest {
    private static final String ADMIN_PASSWORD = "Senha-admin-segura-2026";

    @DynamicPropertySource
    static void admin(DynamicPropertyRegistry registry) {
        registry.add("document-shield.admin.username", () -> "admin-test");
        registry.add("document-shield.admin.password-hash",
                () -> new BCryptPasswordEncoder(12).encode(ADMIN_PASSWORD));
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired IntegracaoApiRepository repository;
    @Autowired ApiKeyHasher hasher;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void rejectsAnonymousPublicTokenAndMissingCsrf() throws Exception {
        mvc.perform(get("/api/v1/admin/auth/me"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.authenticated").value(false));
        mvc.perform(get("/api/v1/admin/api-keys")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/api-keys").header("Authorization", "Bearer dsk_" + "a".repeat(32) + "." + "b".repeat(43)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/auth/login").with(csrf())
                        .param("username", "admin-test").param("password", "incorreta"))
                .andExpect(status().isUnauthorized());

        MockHttpSession session = login();
        mvc.perform(post("/api/v1/admin/api-keys").session(session)
                        .contentType("application/json").content(createBody("Sem CSRF")))
                .andExpect(status().isForbidden());
    }

    @Test
    void createsListsGetsRotatesAndRevokesWithoutLeakingVerifier() throws Exception {
        MockHttpSession session = login();
        MvcResult creation = mvc.perform(post("/api/v1/admin/api-keys").session(session).with(csrf())
                        .contentType("application/json").content(createBody("Sistema Financeiro")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.apiKey.nome").value("Sistema Financeiro"))
                .andExpect(jsonPath("$.apiKey.escopo").value("DOCUMENT_ANALYZE"))
                .andExpect(jsonPath("$.apiKey.status").value("ATIVA"))
                .andExpect(jsonPath("$.token").isString())
                .andReturn();
        JsonNode created = json.readTree(creation.getResponse().getContentAsString());
        String id = created.at("/apiKey/id").asText();
        String token = created.get("token").asText();

        String listing = mvc.perform(get("/api/v1/admin/api-keys").session(session)
                        .queryParam("nome", "Financeiro").queryParam("status", "ATIVA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].prefixo").isString())
                .andReturn().getResponse().getContentAsString();
        assertThat(listing).doesNotContain(token).doesNotContain(hasher.hash(token)).doesNotContain("tokenHash");

        mvc.perform(get("/api/v1/admin/api-keys/{id}", id).session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.descricao").value("ERP corporativo"));

        MvcResult rotation = mvc.perform(post("/api/v1/admin/api-keys/{id}/rotacionar", id)
                        .session(session).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isString()).andReturn();
        String rotatedToken = json.readTree(rotation.getResponse().getContentAsString()).get("token").asText();
        assertThat(rotatedToken).isNotEqualTo(token);
        assertThat(repository.findById(java.util.UUID.fromString(id)).orElseThrow().getRevogadaEm()).isNotNull();

        String replacementId = json.readTree(rotation.getResponse().getContentAsString()).at("/apiKey/id").asText();
        mvc.perform(post("/api/v1/admin/api-keys/{id}/revogar", replacementId).session(session).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REVOGADA"));
        mvc.perform(post("/api/v1/admin/api-keys/{id}/rotacionar", replacementId).session(session).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void exposesSessionStateAndLogsOut() throws Exception {
        MockHttpSession session = login();
        mvc.perform(get("/api/v1/admin/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin-test"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"));
        mvc.perform(post("/api/v1/admin/auth/logout").session(session).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/admin/api-keys").session(session)).andExpect(status().isUnauthorized());
    }

    private MockHttpSession login() throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/admin/auth/login").with(csrf())
                        .param("username", "admin-test").param("password", ADMIN_PASSWORD))
                .andExpect(status().isOk()).andExpect(jsonPath("$.authenticated").value(true)).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String createBody(String name) {
        return "{\"nome\":\"" + name + "\",\"descricao\":\"ERP corporativo\","
                + "\"escopo\":\"DOCUMENT_ANALYZE\",\"requisicoesPorMinuto\":30}";
    }
}
