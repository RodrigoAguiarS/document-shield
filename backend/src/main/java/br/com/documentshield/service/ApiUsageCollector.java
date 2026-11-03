package br.com.documentshield.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
public class ApiUsageCollector {
    private static final Logger log = LoggerFactory.getLogger(ApiUsageCollector.class);
    private final ApiUsageAtomicWriter writer;

    public ApiUsageCollector(ApiUsageAtomicWriter writer) {
        this.writer = writer;
    }

    public void record(UUID integrationId, ApiUsageAtomicWriter.ApiUsageEvent event) {
        write(null, "HOUR", event.occurredAt().truncatedTo(ChronoUnit.HOURS), event);
        write(
                null,
                "DAY",
                event.occurredAt()
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate()
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant(),
                event);
        if (integrationId != null) {
            write(integrationId, "HOUR", event.occurredAt().truncatedTo(ChronoUnit.HOURS), event);
            write(
                    integrationId,
                    "DAY",
                    event.occurredAt()
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                            .atStartOfDay(ZoneOffset.UTC)
                            .toInstant(),
                    event);
        }
    }

    private void write(
            UUID integrationId, String granularity, Instant start, ApiUsageAtomicWriter.ApiUsageEvent event) {
        for (int attempt = 0; attempt < 8; attempt++) {
            try {
                writer.increment(integrationId, granularity, start, event);
                return;
            } catch (ObjectOptimisticLockingFailureException | DataIntegrityViolationException ex) {
                if (attempt == 7) {
                    log.warn("Falha concorrente ao atualizar agregado de uso da API");
                }
            } catch (RuntimeException ex) {
                log.warn("Falha isolada ao registrar métrica agregada da API");
                return;
            }
        }
    }
}
