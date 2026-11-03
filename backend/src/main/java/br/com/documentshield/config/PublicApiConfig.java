package br.com.documentshield.config;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PublicApiConfig {
    @Bean(name = "publicAnalysisExecutor", destroyMethod = "shutdown")
    ThreadPoolExecutor publicAnalysisExecutor(
            @Value("${document-shield.public-api.max-concurrency:2}") int concurrency,
            @Value("${document-shield.public-api.queue-capacity:2}") int queueCapacity) {
        return new ThreadPoolExecutor(
                concurrency,
                concurrency,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(Math.max(1, queueCapacity)),
                Thread.ofPlatform().name("public-analysis-", 0).factory(),
                new ThreadPoolExecutor.AbortPolicy());
    }
}
