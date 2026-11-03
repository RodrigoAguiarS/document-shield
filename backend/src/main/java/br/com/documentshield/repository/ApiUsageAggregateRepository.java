package br.com.documentshield.repository;

import br.com.documentshield.domain.entity.ApiUsageAggregate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApiUsageAggregateRepository extends JpaRepository<ApiUsageAggregate, UUID> {
    Optional<ApiUsageAggregate> findByAggregationKeyAndGranularityAndPeriodStart(
            String aggregationKey, String granularity, Instant periodStart);

    @Query("select a from ApiUsageAggregate a where a.granularity = :granularity "
            + "and a.periodStart >= :start and a.periodStart < :end")
    List<ApiUsageAggregate> findBuckets(
            @Param("granularity") String granularity, @Param("start") Instant start, @Param("end") Instant end);

    long deleteByGranularityAndPeriodStartBefore(String granularity, Instant cutoff);
}
