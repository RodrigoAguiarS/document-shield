package br.com.documentshield.service;

import br.com.documentshield.repository.ApiUsageAggregateRepository;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiUsageRetentionService {
    private final ApiUsageAggregateRepository repository;
    private final int hourlyDays;
    private final int dailyDays;

    public ApiUsageRetentionService(
            ApiUsageAggregateRepository repository,
            @Value("${document-shield.monitoring.retention-hourly-days:90}") int hourlyDays,
            @Value("${document-shield.monitoring.retention-daily-days:365}") int dailyDays) {
        this.repository = repository;
        this.hourlyDays = hourlyDays;
        this.dailyDays = dailyDays;
    }

    @Scheduled(cron = "${document-shield.monitoring.retention-cron:0 17 3 * * *}", zone = "UTC")
    @Transactional
    public void purgeExpiredBuckets() {
        Instant now = Instant.now();
        repository.deleteByGranularityAndPeriodStartBefore("HOUR", now.minus(Duration.ofDays(hourlyDays)));
        repository.deleteByGranularityAndPeriodStartBefore("DAY", now.minus(Duration.ofDays(dailyDays)));
    }
}
