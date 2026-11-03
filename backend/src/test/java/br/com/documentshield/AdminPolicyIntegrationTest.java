package br.com.documentshield;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.documentshield.domain.entity.IntegracaoApi;
import br.com.documentshield.repository.IntegracaoApiRepository;
import br.com.documentshield.repository.IntegrationPolicyRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:admin-policy-test;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class AdminPolicyIntegrationTest {
    private static final String PASSWORD = "Senha-admin-policy-2026";

    @DynamicPropertySource
    static void admin(DynamicPropertyRegistry registry) {
        registry.add("document-shield.admin.username", () -> "policy-admin");
        registry.add("document-shield.admin.password-hash", () -> new BCryptPasswordEncoder(12).encode(PASSWORD));
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    IntegracaoApiRepository integrations;

    @Autowired
    IntegrationPolicyRepository bindings;

    @BeforeEach
    void cleanBindings() {
        bindings.deleteAll();
        integrations.deleteAll();
    }

    @Test
    void protectsAdministrativePolicyOperations() throws Exception {
        mvc.perform(get("/api/v1/admin/politicas")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/politicas")
                        .contentType("application/json")
                        .content(validPolicy("x")))
                .andExpect(status().isForbidden());
    }

    @Test
    void createsActivatesVersionsSimulatesBindsAndAudits() throws Exception {
        MockHttpSession session = login();
        mvc.perform(get("/api/v1/admin/politicas").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.tipo == 'GLOBAL' && @.status == 'ATIVA')]")
                        .exists());

        MvcResult createdResult = mvc.perform(post("/api/v1/admin/politicas")
                        .session(session)
                        .with(csrf())
                        .contentType("application/json")
                        .content(validPolicy("Política Financeira")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RASCUNHO"))
                .andReturn();
        JsonNode created = json.readTree(createdResult.getResponse().getContentAsString());
        String policyId = created.get("id").asText();

        mvc.perform(post("/api/v1/admin/politicas/{id}/ativar", policyId)
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATIVA"));
        mvc.perform(post("/api/v1/admin/politicas/{id}/simular", policyId)
                        .session(session)
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"pontuacao\":40}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decisao").value("BLOCK"))
                .andExpect(jsonPath("$.regraAplicada.pontuacaoMinima").value(40));

        UUID integrationId = integrations
                .save(IntegracaoApi.builder()
                        .keyId(UUID.randomUUID().toString().replace("-", ""))
                        .tokenHash("a".repeat(64))
                        .nome("ERP Financeiro")
                        .escopo("DOCUMENT_ANALYZE")
                        .ativa(true)
                        .requisicoesPorMinuto(30)
                        .build())
                .getId();
        mvc.perform(put("/api/v1/admin/integracoes/{id}/politica", integrationId)
                        .session(session)
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"politicaId\":\"" + policyId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utilizaPoliticaGlobal").value(false));

        MvcResult versionResult = mvc.perform(put("/api/v1/admin/politicas/{id}", policyId)
                        .session(session)
                        .with(csrf())
                        .contentType("application/json")
                        .content(validPolicy("Política Financeira")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versao").value(2))
                .andExpect(jsonPath("$.status").value("RASCUNHO"))
                .andReturn();
        String versionId = json.readTree(versionResult.getResponse().getContentAsString())
                .get("id")
                .asText();
        mvc.perform(post("/api/v1/admin/politicas/{id}/ativar", versionId)
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/integracoes/{id}/politica", integrationId)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.politicaId").value(versionId))
                .andExpect(jsonPath("$.versao").value(2));
        mvc.perform(delete("/api/v1/admin/integracoes/{id}/politica", integrationId)
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utilizaPoliticaGlobal").value(true));
        mvc.perform(get("/api/v1/admin/politicas/auditoria").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].operacao").exists());
    }

    @Test
    void refusesActivationWithIncompleteIntervals() throws Exception {
        MockHttpSession session = login();
        String body = "{\"nome\":\"Inválida\",\"tipo\":\"INTEGRACAO\",\"regras\":["
                + "{\"pontuacaoMinima\":0,\"pontuacaoMaxima\":29,\"decisao\":\"ALLOW\",\"prioridade\":1},"
                + "{\"pontuacaoMinima\":40,\"pontuacaoMaxima\":100,\"decisao\":\"BLOCK\",\"prioridade\":2}]}";
        MvcResult result = mvc.perform(post("/api/v1/admin/politicas")
                        .session(session)
                        .with(csrf())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        String id = json.readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText();
        mvc.perform(post("/api/v1/admin/politicas/{id}/ativar", id)
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    private MockHttpSession login() throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/admin/auth/login")
                        .with(csrf())
                        .param("username", "policy-admin")
                        .param("password", PASSWORD))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String validPolicy(String name) {
        return "{\"nome\":\"" + name + "\",\"descricao\":\"Controle por risco\",\"tipo\":\"INTEGRACAO\",\"regras\":["
                + "{\"pontuacaoMinima\":0,\"pontuacaoMaxima\":19,\"decisao\":\"ALLOW\",\"prioridade\":1},"
                + "{\"pontuacaoMinima\":20,\"pontuacaoMaxima\":39,\"decisao\":\"REVIEW\",\"prioridade\":2},"
                + "{\"pontuacaoMinima\":40,\"pontuacaoMaxima\":100,\"decisao\":\"BLOCK\",\"prioridade\":3}]}";
    }
}
