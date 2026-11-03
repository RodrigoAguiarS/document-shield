package br.com.documentshield;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:openapi;DB_CLOSE_DELAY=-1",
            "document-shield.storage-directory=${java.io.tmpdir}/document-shield-openapi"
        })
@AutoConfigureMockMvc
class OpenApiExportTest {
    @Autowired
    MockMvc mvc;

    @Test
    void exportaContratoDaAplicacao() throws Exception {
        String json = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Path target =
                Path.of("..", "docs", "openapi", "document-shield-v1.json").normalize();
        Files.createDirectories(target.getParent());
        Files.writeString(target, json);
    }
}
