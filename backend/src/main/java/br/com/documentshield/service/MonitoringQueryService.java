package br.com.documentshield.service;

import br.com.documentshield.domain.entity.ApiUsageAggregate;
import br.com.documentshield.dto.response.MonitoringResponses.Capacity;
import br.com.documentshield.dto.response.MonitoringResponses.ErrorItem;
import br.com.documentshield.dto.response.MonitoringResponses.Errors;
import br.com.documentshield.dto.response.MonitoringResponses.Integration;
import br.com.documentshield.dto.response.MonitoringResponses.IntegrationList;
import br.com.documentshield.dto.response.MonitoringResponses.Summary;
import br.com.documentshield.dto.response.MonitoringResponses.TemporalPoint;
import br.com.documentshield.dto.response.MonitoringResponses.TimeSeries;
import br.com.documentshield.repository.ApiUsageAggregateRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MonitoringQueryService {
    private final ApiUsageAggregateRepository repository;
    private final ApiKeyManagementService apiKeys;
    private final ThreadPoolExecutor executor;
    private final JdbcTemplate jdbc;

    public MonitoringQueryService(
            ApiUsageAggregateRepository repository,
            ApiKeyManagementService apiKeys,
            @Qualifier("publicAnalysisExecutor") ThreadPoolExecutor executor,
            JdbcTemplate jdbc) {
        this.repository = repository;
        this.apiKeys = apiKeys;
        this.executor = executor;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public Summary summary(Window window, UUID integrationId) {
        List<ApiUsageAggregate> rows = selected(
                window,
                integrationId == null
                        ? row -> row.getIntegrationId() == null
                        : row -> integrationId.equals(row.getIntegrationId()));
        Totals total = totals(rows);
        long integrations = integrationId == null
                ? selected(window, row -> row.getIntegrationId() != null).stream()
                        .map(ApiUsageAggregate::getIntegrationId)
                        .distinct()
                        .count()
                : total.requests > 0 ? 1 : 0;
        return new Summary(
                window.start,
                window.end,
                window.granularity,
                total.requests,
                total.success,
                total.failures(),
                rate(total.success, eligible(total)),
                average(total.sumDuration, total.durationCount),
                percentile95(total),
                integrations);
    }

    @Transactional(readOnly = true)
    public TimeSeries timeSeries(Window window, UUID integrationId) {
        Predicate<ApiUsageAggregate> predicate = integrationId == null
                ? row -> row.getIntegrationId() == null
                : row -> integrationId.equals(row.getIntegrationId());
        Map<Instant, Totals> byStart = selected(window, predicate).stream()
                .collect(Collectors.groupingBy(
                        ApiUsageAggregate::getPeriodStart,
                        TreeMap::new,
                        Collectors.collectingAndThen(Collectors.toList(), this::totals)));
        List<TemporalPoint> points = new ArrayList<>();
        for (Instant cursor = bucketStart(window.start, window.granularity);
                cursor.isBefore(window.end);
                cursor = advance(cursor, window.granularity)) {
            Totals total = byStart.getOrDefault(cursor, new Totals());
            points.add(new TemporalPoint(
                    cursor,
                    total.requests,
                    total.success,
                    total.failures(),
                    average(total.sumDuration, total.durationCount)));
        }
        return new TimeSeries(window.start, window.end, window.granularity, points);
    }

    @Transactional(readOnly = true)
    public IntegrationList integrations(Window window) {
        Map<UUID, List<ApiUsageAggregate>> grouped = selected(window, row -> row.getIntegrationId() != null).stream()
                .collect(Collectors.groupingBy(ApiUsageAggregate::getIntegrationId));
        Map<UUID, ApiKeyManagementService.ApiKeyView> views = apiKeys.list().stream()
                .collect(Collectors.toMap(ApiKeyManagementService.ApiKeyView::id, value -> value));
        List<Integration> values = grouped.entrySet().stream()
                .map(entry -> {
                    Totals total = totals(entry.getValue());
                    ApiKeyManagementService.ApiKeyView view = views.get(entry.getKey());
                    String name = view == null ? "Integração removida" : view.name();
                    String status = view == null ? "INATIVA" : status(view);
                    return new Integration(
                            entry.getKey(),
                            name,
                            status,
                            total.requests,
                            total.success,
                            total.failures(),
                            rate(total.success, eligible(total)),
                            average(total.sumDuration, total.durationCount),
                            percentile95(total),
                            view == null ? null : view.requestsPerMinute());
                })
                .sorted(Comparator.comparingLong(Integration::totalRequisicoes).reversed())
                .toList();
        return new IntegrationList(window.start, window.end, values);
    }

    @Transactional(readOnly = true)
    public Integration integration(Window window, UUID id) {
        return integrations(window).integracoes().stream()
                .filter(value -> value.id().equals(id))
                .findFirst()
                .orElseGet(() -> {
                    ApiKeyManagementService.ApiKeyView view = apiKeys.get(id);
                    return new Integration(id, view.name(), status(view), 0, 0, 0, 0, 0, 0, view.requestsPerMinute());
                });
    }

    @Transactional(readOnly = true)
    public Errors errors(Window window, UUID integrationId) {
        List<ApiUsageAggregate> rows = selected(
                window,
                integrationId == null
                        ? row -> row.getIntegrationId() == null
                        : row -> integrationId.equals(row.getIntegrationId()));
        Totals total = totals(rows);
        long failures = total.failures();
        List<ErrorItem> items = List.of(
                error("AUTENTICACAO", total.authFailures, failures),
                error("LIMITE_DE_TAXA", total.rateLimit, failures),
                error("VALIDACAO", total.validationErrors, failures),
                error("TIMEOUT", total.timeouts, failures),
                error("PROCESSAMENTO", total.processingFailures, failures),
                error(
                        "OUTROS_4XX",
                        Math.max(0, total.clientErrors - total.authFailures - total.rateLimit - total.validationErrors),
                        failures),
                error(
                        "OUTROS_5XX",
                        Math.max(0, total.serverErrors - total.timeouts - total.processingFailures),
                        failures));
        return new Errors(
                window.start,
                window.end,
                failures,
                items.stream().filter(item -> item.quantidade() > 0).toList());
    }

    public Capacity capacity() {
        String database;
        try {
            database = Objects.equals(jdbc.queryForObject("SELECT 1", Integer.class), 1) ? "UP" : "DEGRADADO";
        } catch (RuntimeException ex) {
            database = "DOWN";
        }
        int capacity = executor.getQueue().size() + executor.getQueue().remainingCapacity();
        int active = executor.getActiveCount();
        return new Capacity(
                database.equals("UP") && !executor.isShutdown() ? "UP" : "DEGRADADO",
                database,
                executor.getMaximumPoolSize(),
                active,
                Math.max(0, executor.getMaximumPoolSize() - active),
                executor.getQueue().size(),
                capacity,
                executor.getQueue().remainingCapacity(),
                Instant.now());
    }

    public Window resolve(String period, Instant start, Instant end, String requestedGranularity) {
        Instant resolvedEnd = end == null ? Instant.now() : end;
        Instant resolvedStart = switch (period == null ? "24H" : period.toUpperCase(Locale.ROOT)) {
            case "24H" -> resolvedEnd.minus(24, ChronoUnit.HOURS);
            case "7D" -> resolvedEnd.minus(7, ChronoUnit.DAYS);
            case "30D" -> resolvedEnd.minus(30, ChronoUnit.DAYS);
            case "CUSTOM" -> {
                if (start == null || end == null) {
                    throw new IllegalArgumentException("Informe dataInicio e dataFim no período personalizado.");
                }
                yield start;
            }
            default -> throw new IllegalArgumentException("Período inválido. Use 24H, 7D, 30D ou CUSTOM.");
        };
        if (!resolvedStart.isBefore(resolvedEnd)) {
            throw new IllegalArgumentException("dataInicio deve ser anterior a dataFim.");
        }
        if (Duration.between(resolvedStart, resolvedEnd).toDays() > 366) {
            throw new IllegalArgumentException("O período máximo para consulta é de 366 dias.");
        }
        String granularity = requestedGranularity == null || requestedGranularity.isBlank()
                ? (Duration.between(resolvedStart, resolvedEnd).toHours() <= 48 ? "HOUR" : "DAY")
                : requestedGranularity.toUpperCase(Locale.ROOT);
        if (!granularity.equals("HOUR") && !granularity.equals("DAY")) {
            throw new IllegalArgumentException("Granularidade inválida. Use HOUR ou DAY.");
        }
        return new Window(resolvedStart, resolvedEnd, granularity);
    }

    private List<ApiUsageAggregate> selected(Window window, Predicate<ApiUsageAggregate> predicate) {
        Instant queryStart = bucketStart(window.start, window.granularity);
        return repository.findBuckets(window.granularity, queryStart, window.end).stream()
                .filter(predicate)
                .toList();
    }

    private Totals totals(List<ApiUsageAggregate> rows) {
        Totals value = new Totals();
        rows.forEach(row -> value.add(row));
        return value;
    }

    private Instant bucketStart(Instant value, String granularity) {
        if (granularity.equals("HOUR")) {
            return value.truncatedTo(ChronoUnit.HOURS);
        }
        return value.atZone(ZoneOffset.UTC)
                .toLocalDate()
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
    }

    private Instant advance(Instant value, String granularity) {
        return granularity.equals("HOUR") ? value.plus(1, ChronoUnit.HOURS) : value.plus(1, ChronoUnit.DAYS);
    }

    private String status(ApiKeyManagementService.ApiKeyView view) {
        if (!view.active() || view.revokedAt() != null) {
            return "REVOGADA";
        }
        if (view.expiresAt() != null && !view.expiresAt().isAfter(Instant.now())) {
            return "EXPIRADA";
        }
        return "ATIVA";
    }

    private long eligible(Totals total) {
        return Math.max(0, total.requests - total.authFailures - total.rateLimit);
    }

    private double rate(long numerator, long denominator) {
        return denominator == 0 ? 0 : round(numerator * 100.0 / denominator);
    }

    private double average(long sum, long count) {
        return count == 0 ? 0 : round(sum * 1.0 / count);
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private ErrorItem error(String type, long amount, long failures) {
        return new ErrorItem(type, amount, rate(amount, failures));
    }

    private long percentile95(Totals total) {
        if (total.durationCount == 0) {
            return 0;
        }
        long target = (long) Math.ceil(total.durationCount * .95);
        long cumulative = 0;
        long[] limits = {100, 250, 500, 1_000, 2_500, 5_000, 10_000, 30_000, total.maxDuration};
        for (int i = 0; i < total.buckets.length; i++) {
            cumulative += total.buckets[i];
            if (cumulative >= target) {
                return limits[i];
            }
        }
        return total.maxDuration;
    }

    public record Window(Instant start, Instant end, String granularity) {}

    private static class Totals {
        long requests, success, clientErrors, serverErrors, rateLimit, authFailures, timeouts;
        long validationErrors, processingFailures, sumDuration, durationCount, maxDuration;
        long[] buckets = new long[9];

        void add(ApiUsageAggregate row) {
            requests += row.getTotalRequests();
            success += row.getTotalSuccess();
            clientErrors += row.getTotalClientErrors();
            serverErrors += row.getTotalServerErrors();
            rateLimit += row.getTotalRateLimit();
            authFailures += row.getTotalAuthFailures();
            timeouts += row.getTotalTimeouts();
            validationErrors += row.getTotalValidationErrors();
            processingFailures += row.getTotalProcessingFailures();
            sumDuration += row.getSumDurationMs();
            durationCount += row.getDurationCount();
            maxDuration = Math.max(maxDuration, row.getMaxDurationMs());
            long[] source = {
                row.getBucket100(),
                row.getBucket250(),
                row.getBucket500(),
                row.getBucket1000(),
                row.getBucket2500(),
                row.getBucket5000(),
                row.getBucket10000(),
                row.getBucket30000(),
                row.getBucketOver30000()
            };
            for (int i = 0; i < source.length; i++) {
                buckets[i] += source[i];
            }
        }

        long failures() {
            return Math.max(0, requests - success);
        }
    }
}
