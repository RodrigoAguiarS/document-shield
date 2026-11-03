package br.com.documentshield.security;

import br.com.documentshield.service.ApiUsageAtomicWriter;
import br.com.documentshield.service.ApiUsageCollector;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class PublicApiMonitoringFilter extends OncePerRequestFilter {
    public static final String PROCESSING_DURATION_ATTRIBUTE = "documentShield.publicApiProcessingDurationMs";
    private final ApiUsageCollector collector;

    public PublicApiMonitoringFilter(ApiUsageCollector collector) {
        this.collector = collector;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/public/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long started = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            Object id = request.getAttribute(PublicApiKeyFilter.CREDENTIAL_ATTRIBUTE);
            Object processing = request.getAttribute(PROCESSING_DURATION_ATTRIBUTE);
            long duration = (System.nanoTime() - started) / 1_000_000;
            collector.record(
                    id instanceof UUID uuid ? uuid : null,
                    new ApiUsageAtomicWriter.ApiUsageEvent(
                            Instant.now(),
                            response.getStatus(),
                            duration,
                            processing instanceof Number number ? number.longValue() : null));
        }
    }
}
