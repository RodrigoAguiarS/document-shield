package br.com.documentshield;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.documentshield.domain.entity.ApiUsageAggregate;
import br.com.documentshield.repository.ApiUsageAggregateRepository;
import br.com.documentshield.service.ApiUsageAtomicWriter;
import br.com.documentshield.service.ApiUsageCollector;
import br.com.documentshield.service.MonitoringQueryService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:monitoring-test;DB_CLOSE_DELAY=-1",
            "spring.jpa.hibernate.ddl-auto=none",
            "spring.sql.init.mode=always",
            "logging.level.org.hibernate.SQL=OFF"
        })
class ApiUsageMonitoringIntegrationTest {
    @Autowired
    ApiUsageCollector collector;

    @Autowired
    ApiUsageAggregateRepository repository;

    @Autowired
    MonitoringQueryService query;

    @BeforeEach
    void clear() {
        repository.deleteAll();
    }

    @Test
    void aggregatesGloballyAndByIntegrationWithoutRequestRecords() {
        UUID integrationId = UUID.randomUUID();
        Instant now = Instant.now();
        collector.record(integrationId, event(now, 200, 80));
        collector.record(integrationId, event(now, 429, 150));
        collector.record(null, event(now, 401, 600));

        List<ApiUsageAggregate> hourly = repository.findBuckets("HOUR", now.minusSeconds(3600), now.plusSeconds(3600));
        ApiUsageAggregate global = hourly.stream()
                .filter(value -> value.getIntegrationId() == null)
                .findFirst()
                .orElseThrow();
        ApiUsageAggregate integration = hourly.stream()
                .filter(value -> integrationId.equals(value.getIntegrationId()))
                .findFirst()
                .orElseThrow();

        assertThat(global.getTotalRequests()).isEqualTo(3);
        assertThat(global.getTotalSuccess()).isEqualTo(1);
        assertThat(global.getTotalAuthFailures()).isEqualTo(1);
        assertThat(global.getTotalRateLimit()).isEqualTo(1);
        assertThat(integration.getTotalRequests()).isEqualTo(2);
        assertThat(repository.count()).isEqualTo(4); // global + integração, hora + dia

        MonitoringQueryService.Window window = query.resolve("24H", null, null, null);
        var summary = query.summary(window, null);
        assertThat(summary.totalRequisicoes()).isEqualTo(3);
        assertThat(summary.taxaSucesso()).isEqualTo(100.0); // 401 e 429 não compõem o denominador elegível
        assertThat(summary.p95Ms()).isEqualTo(1_000);
    }

    @Test
    void keepsExactCountUnderConcurrentUpdates() throws Exception {
        UUID integrationId = UUID.randomUUID();
        Instant now = Instant.now();
        int calls = 12;
        try (ExecutorService pool = Executors.newFixedThreadPool(4)) {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < calls; i++) {
                futures.add(pool.submit(() -> collector.record(integrationId, event(now, 200, 25))));
            }
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        }
        ApiUsageAggregate global =
                repository.findBuckets("HOUR", now.minusSeconds(3600), now.plusSeconds(3600)).stream()
                        .filter(value -> value.getIntegrationId() == null)
                        .findFirst()
                        .orElseThrow();
        assertThat(global.getTotalRequests()).isEqualTo(calls);
        assertThat(global.getTotalSuccess()).isEqualTo(calls);
    }

    private ApiUsageAtomicWriter.ApiUsageEvent event(Instant at, int status, long duration) {
        return new ApiUsageAtomicWriter.ApiUsageEvent(at, status, duration, status == 200 ? duration - 5 : null);
    }
}
