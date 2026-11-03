package br.com.documentshield.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {
    @Bean("analysisExecutor")
    public TaskExecutor analysisExecutor(
            @Value("${document-shield.processing.core-pool-size:2}") int core,
            @Value("${document-shield.processing.max-pool-size:2}") int max,
            @Value("${document-shield.processing.queue-capacity:25}") int queue) {
        ThreadPoolTaskExecutor e = new ThreadPoolTaskExecutor();
        e.setCorePoolSize(core);
        e.setMaxPoolSize(max);
        e.setQueueCapacity(queue);
        e.setThreadNamePrefix("document-analysis-");
        e.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        e.setWaitForTasksToCompleteOnShutdown(true);
        e.setAwaitTerminationSeconds(30);
        e.initialize();
        return e;
    }
}
