package com.discover.billpay.migration.batch.config;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.infrastructure.repeat.RepeatOperations;
import org.springframework.batch.infrastructure.repeat.support.TaskExecutorRepeatTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.VirtualThreadTaskExecutor;

@Configuration
@RequiredArgsConstructor
public class BatchInfrastructureConfig {

    private final MigrationProperties migrationProperties;

    @Bean
    public TaskExecutor migrationTaskExecutor() {
        var virtualThreadTaskExecutor = new VirtualThreadTaskExecutor("billpay-batch-");
        var permits = new Semaphore(migrationProperties.getConcurrency());

        return task -> {
            try {
                permits.acquire();
                virtualThreadTaskExecutor.execute(() -> {
                    try {
                        task.run();
                    } finally {
                        permits.release();
                    }
                });
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RejectedExecutionException("Interrupted while waiting for batch concurrency permit", ex);
            }
        };
    }

    @Bean
    public RepeatOperations migrationRepeatOperations(TaskExecutor migrationTaskExecutor) {
        var repeatTemplate = new TaskExecutorRepeatTemplate();
        repeatTemplate.setTaskExecutor(migrationTaskExecutor);
        return repeatTemplate;
    }
}
