package br.com.documentshield.security;

import br.com.documentshield.domain.entity.IntegracaoApi;
import br.com.documentshield.service.ApiKeyManagementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.regex.*;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class PublicApiKeyFilter extends OncePerRequestFilter {
    public static final String CREDENTIAL_ATTRIBUTE = "documentShield.publicApiCredentialId";
    private static final Pattern FORMAT = Pattern.compile("dsk_([0-9a-fA-F]{32})\\.([A-Za-z0-9_-]{40,})");
    private final ApiKeyManagementService apiKeys;
    private final PublicApiRateLimiter limiter;
    private final ObjectMapper json;
    private final int defaultRateLimit;

    public PublicApiKeyFilter(ApiKeyManagementService apiKeys,
            PublicApiRateLimiter limiter, ObjectMapper json,
            @Value("${document-shield.public-api.rate-limit-per-minute:30}") int defaultRateLimit) {
        this.apiKeys = apiKeys;
        this.limiter = limiter;
        this.json = json;
        this.defaultRateLimit = defaultRateLimit;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/public/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            problem(response, HttpStatus.UNAUTHORIZED, "Credencial obrigatória", "Informe uma API Key no cabeçalho Authorization.");
            return;
        }
        String token = authorization.substring(7).trim();
        Matcher matcher = FORMAT.matcher(token);
        if (!matcher.matches()) {
            problem(response, HttpStatus.UNAUTHORIZED, "Credencial inválida", "A credencial informada não é válida.");
            return;
        }
        ApiKeyManagementService.AuthenticationResult authentication = apiKeys.authenticate(token,
                matcher.group(1).toLowerCase(), ApiKeyManagementService.DOCUMENT_ANALYZE_SCOPE, Instant.now());
        if (authentication.status() == ApiKeyManagementService.AuthenticationStatus.INVALID) {
            problem(response, HttpStatus.UNAUTHORIZED, "Credencial inválida", "A credencial informada não é válida ou não está ativa.");
            return;
        }
        if (authentication.status() == ApiKeyManagementService.AuthenticationStatus.FORBIDDEN) {
            problem(response, HttpStatus.FORBIDDEN, "Escopo insuficiente", "A credencial não permite análise pública.");
            return;
        }
        IntegracaoApi credential = authentication.credential();
        request.setAttribute(CREDENTIAL_ATTRIBUTE, credential.getId());
        int limit = credential.getRequisicoesPorMinuto() == null ? defaultRateLimit : credential.getRequisicoesPorMinuto();
        if (!limiter.allow(credential.getId(), limit)) {
            response.setHeader(HttpHeaders.RETRY_AFTER, "60");
            problem(response, HttpStatus.TOO_MANY_REQUESTS, "Limite excedido", "Aguarde antes de enviar uma nova análise.");
            return;
        }
        chain.doFilter(request, response);
    }

    private void problem(HttpServletResponse response, HttpStatus status, String title, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("urn:document-shield:public-api:" + status.value()));
        problem.setProperty("timestamp", Instant.now());
        json.writeValue(response.getOutputStream(), problem);
    }
}
