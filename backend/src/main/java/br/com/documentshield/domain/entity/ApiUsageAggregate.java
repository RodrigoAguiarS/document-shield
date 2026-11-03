package br.com.documentshield.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "api_usage_aggregate",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_api_usage_bucket",
                        columnNames = {"aggregation_key", "granularity", "period_start"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiUsageAggregate {
    @Id
    private UUID id;

    @Column(name = "integration_id")
    private UUID integrationId;

    @Column(name = "aggregation_key", nullable = false, length = 36)
    private String aggregationKey;

    @Column(nullable = false, length = 8)
    private String granularity;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "total_requests", nullable = false)
    private long totalRequests;

    @Column(name = "total_success", nullable = false)
    private long totalSuccess;

    @Column(name = "total_client_errors", nullable = false)
    private long totalClientErrors;

    @Column(name = "total_server_errors", nullable = false)
    private long totalServerErrors;

    @Column(name = "total_rate_limit", nullable = false)
    private long totalRateLimit;

    @Column(name = "total_auth_failures", nullable = false)
    private long totalAuthFailures;

    @Column(name = "total_timeouts", nullable = false)
    private long totalTimeouts;

    @Column(name = "total_validation_errors", nullable = false)
    private long totalValidationErrors;

    @Column(name = "total_processing_failures", nullable = false)
    private long totalProcessingFailures;

    @Column(name = "sum_duration_ms", nullable = false)
    private long sumDurationMs;

    @Column(name = "duration_count", nullable = false)
    private long durationCount;

    @Column(name = "sum_processing_ms", nullable = false)
    private long sumProcessingMs;

    @Column(name = "processing_count", nullable = false)
    private long processingCount;

    @Column(name = "bucket_100", nullable = false)
    private long bucket100;

    @Column(name = "bucket_250", nullable = false)
    private long bucket250;

    @Column(name = "bucket_500", nullable = false)
    private long bucket500;

    @Column(name = "bucket_1000", nullable = false)
    private long bucket1000;

    @Column(name = "bucket_2500", nullable = false)
    private long bucket2500;

    @Column(name = "bucket_5000", nullable = false)
    private long bucket5000;

    @Column(name = "bucket_10000", nullable = false)
    private long bucket10000;

    @Column(name = "bucket_30000", nullable = false)
    private long bucket30000;

    @Column(name = "bucket_over_30000", nullable = false)
    private long bucketOver30000;

    @Column(name = "max_duration_ms", nullable = false)
    private long maxDurationMs;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }
}
