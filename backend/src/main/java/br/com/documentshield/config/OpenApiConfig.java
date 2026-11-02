package br.com.documentshield.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.*;
import java.util.List;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI api() {
        return new OpenAPI().info(new Info().title("Document Shield API").version("v1").description(
                "API para análise defensiva de PDFs. A rota pública exige API Key; as rotas administrativas permanecem restritas ao ambiente local."))
                .components(new Components().addSecuritySchemes("publicApiKey", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer")
                        .bearerFormat("Document Shield API Key"))
                        .addSecuritySchemes("adminSession", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.COOKIE).name("JSESSIONID")))
                .servers(List.of(new Server().url("http://localhost:8080")));
    }
}
