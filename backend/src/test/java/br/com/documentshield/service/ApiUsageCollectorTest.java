package br.com.documentshield.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ApiUsageCollectorTest {
    @Test
    void metricFailureNeverAffectsPublicRequest() {
        ApiUsageAtomicWriter writer = mock(ApiUsageAtomicWriter.class);
        doThrow(new IllegalStateException("database unavailable"))
                .when(writer)
                .increment(any(), anyString(), any(), any());
        ApiUsageCollector collector = new ApiUsageCollector(writer);

        assertThatCode(() -> collector.record(null, new ApiUsageAtomicWriter.ApiUsageEvent(Instant.now(), 200, 10, 8L)))
                .doesNotThrowAnyException();
    }
}
