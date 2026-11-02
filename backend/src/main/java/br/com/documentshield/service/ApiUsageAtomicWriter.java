package br.com.documentshield.service;

import br.com.documentshield.domain.entity.ApiUsageAggregate;
import br.com.documentshield.repository.ApiUsageAggregateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.Instant;
import java.util.UUID;

@Service
public class ApiUsageAtomicWriter {
    private final ApiUsageAggregateRepository repository;

    public ApiUsageAtomicWriter(ApiUsageAggregateRepository repository) { this.repository = repository; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void increment(UUID integrationId, String granularity, Instant periodStart, ApiUsageEvent event) {
        String key = integrationId == null ? "GLOBAL" : integrationId.toString();
        ApiUsageAggregate value = repository
                .findByAggregationKeyAndGranularityAndPeriodStart(key, granularity, periodStart)
                .orElseGet(() -> ApiUsageAggregate.builder().integrationId(integrationId).aggregationKey(key)
                        .granularity(granularity).periodStart(periodStart).updatedAt(event.occurredAt()).build());
        value.setTotalRequests(value.getTotalRequests() + 1);
        int status = event.status();
        if (status >= 200 && status < 300) value.setTotalSuccess(value.getTotalSuccess() + 1);
        if (status >= 400 && status < 500) value.setTotalClientErrors(value.getTotalClientErrors() + 1);
        if (status >= 500) value.setTotalServerErrors(value.getTotalServerErrors() + 1);
        if (status == 429) value.setTotalRateLimit(value.getTotalRateLimit() + 1);
        if (status == 401 || status == 403) value.setTotalAuthFailures(value.getTotalAuthFailures() + 1);
        if (status == 408 || status == 504) value.setTotalTimeouts(value.getTotalTimeouts() + 1);
        if (status == 400 || status == 413 || status == 415 || status == 422)
            value.setTotalValidationErrors(value.getTotalValidationErrors() + 1);
        if (status == 500 || status == 502 || status == 503)
            value.setTotalProcessingFailures(value.getTotalProcessingFailures() + 1);
        long duration = Math.max(0, event.durationMs());
        value.setSumDurationMs(value.getSumDurationMs() + duration);
        value.setDurationCount(value.getDurationCount() + 1);
        value.setMaxDurationMs(Math.max(value.getMaxDurationMs(), duration));
        incrementHistogram(value, duration);
        if (event.processingDurationMs() != null) {
            value.setSumProcessingMs(value.getSumProcessingMs() + Math.max(0, event.processingDurationMs()));
            value.setProcessingCount(value.getProcessingCount() + 1);
        }
        value.setUpdatedAt(event.occurredAt());
        repository.saveAndFlush(value);
    }

    private void incrementHistogram(ApiUsageAggregate value, long duration) {
        if (duration <= 100) value.setBucket100(value.getBucket100() + 1);
        else if (duration <= 250) value.setBucket250(value.getBucket250() + 1);
        else if (duration <= 500) value.setBucket500(value.getBucket500() + 1);
        else if (duration <= 1_000) value.setBucket1000(value.getBucket1000() + 1);
        else if (duration <= 2_500) value.setBucket2500(value.getBucket2500() + 1);
        else if (duration <= 5_000) value.setBucket5000(value.getBucket5000() + 1);
        else if (duration <= 10_000) value.setBucket10000(value.getBucket10000() + 1);
        else if (duration <= 30_000) value.setBucket30000(value.getBucket30000() + 1);
        else value.setBucketOver30000(value.getBucketOver30000() + 1);
    }

    public record ApiUsageEvent(Instant occurredAt, int status, long durationMs, Long processingDurationMs) {}
}
