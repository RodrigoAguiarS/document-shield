package br.com.documentshield.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
public class AdminSecurityConfig {
    @Bean
    PasswordEncoder adminPasswordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    UserDetailsService adminUserDetailsService(
            @Value("${document-shield.admin.username:}") String username,
            @Value("${document-shield.admin.password-hash:}") String passwordHash,
            PasswordEncoder encoder) {
        boolean configured = !username.isBlank() && !passwordHash.isBlank();
        if (configured && !passwordHash.matches("^\\$2[aby]\\$\\d{2}\\$.{53}$")) {
            throw new IllegalStateException("DOCUMENT_SHIELD_ADMIN_PASSWORD_HASH deve conter um hash BCrypt válido.");
        }
        UserDetails admin = User.withUsername(configured ? username : "__admin_not_configured__")
                .password(
                        configured
                                ? passwordHash
                                : encoder.encode(UUID.randomUUID().toString()))
                .roles("ADMIN")
                .disabled(!configured)
                .build();
        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    SecurityFilterChain adminSecurity(
            HttpSecurity http,
            ObjectMapper json,
            @Value("${document-shield.admin.secure-cookies:false}") boolean secureCookies,
            Environment environment)
            throws Exception {
        boolean developmentProfile =
                Arrays.asList(environment.getActiveProfiles()).contains("dev");
        CookieCsrfTokenRepository csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrf.setCookieCustomizer(cookie -> cookie.path("/").sameSite("Strict").secure(secureCookies));
        http.csrf(config -> {
                    config.csrfTokenRepository(csrf)
                            .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                            .ignoringRequestMatchers(
                                    "/api/v1/public/**",
                                    "/api/v1/documentos/**",
                                    "/api/v1/analises/**",
                                    "/api/v1/ocorrencias/**",
                                    "/api/v1/regras/**",
                                    "/api/v1/dashboard/**");
                    if (developmentProfile) {
                        config.ignoringRequestMatchers(PathRequest.toH2Console());
                    }
                })
                .authorizeHttpRequests(auth -> {
                    if (developmentProfile) {
                        auth.requestMatchers(PathRequest.toH2Console()).permitAll();
                    }
                    auth.requestMatchers("/api/v1/admin/auth/csrf", "/api/v1/admin/auth/login", "/api/v1/admin/auth/me")
                            .permitAll()
                            .requestMatchers("/api/v1/admin/**")
                            .hasRole("ADMIN")
                            .anyRequest()
                            .permitAll();
                })
                .formLogin(form -> form.loginProcessingUrl("/api/v1/admin/auth/login")
                        .successHandler((request, response, authentication) -> {
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setHeader("Cache-Control", "no-store");
                            json.writeValue(
                                    response.getOutputStream(),
                                    Map.of("username", authentication.getName(), "authenticated", true));
                        })
                        .failureHandler((request, response, exception) ->
                                problem(json, response, 401, "Autenticação inválida", "Usuário ou senha inválidos.")))
                .logout(logout -> logout.logoutUrl("/api/v1/admin/auth/logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_NO_CONTENT)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fixation -> fixation.migrateSession()))
                .requestCache(cache -> cache.disable())
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) -> problem(
                                json,
                                response,
                                401,
                                "Autenticação administrativa obrigatória",
                                "Inicie uma sessão administrativa para acessar este recurso."))
                        .accessDeniedHandler((request, response, exception) -> problem(
                                json,
                                response,
                                403,
                                "Acesso negado",
                                "A sessão não possui permissão ou o token CSRF é inválido.")));
        if (developmentProfile) {
            http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        }
        return http.build();
    }

    private void problem(ObjectMapper json, HttpServletResponse response, int status, String title, String detail)
            throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader("Cache-Control", "no-store");
        json.writeValue(
                response.getOutputStream(),
                Map.of(
                        "type", "urn:document-shield:admin:" + status,
                        "title", title,
                        "status", status,
                        "detail", detail,
                        "timestamp", Instant.now().toString()));
    }
}
