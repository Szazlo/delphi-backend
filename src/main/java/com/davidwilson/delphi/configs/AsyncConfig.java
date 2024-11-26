package com.davidwilson.delphi.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {
    @Value("${submission.executor.corePoolSize}")
    private int corePoolSize;

    @Value("${submission.executor.maxPoolSize}")
    private int maxPoolSize;

    @Value("${submission.executor.queueCapacity}")
    private int queueCapacity;

    @Bean(name = "submissionExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("FileExecution-");
        executor.initialize();
        return executor;
    }
}