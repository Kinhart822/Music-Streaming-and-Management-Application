package com.spring.config;

import com.spring.service.impl.SongServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {
    private static final Logger log = LoggerFactory.getLogger(SongServiceImpl.class);

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);        // Tối thiểu 4 thread
        executor.setMaxPoolSize(4);         // Tối đa 4 thread
        executor.setQueueCapacity(100);      // Có thể xếp hàng 100 tác vụ nếu thread bận
        executor.setThreadNamePrefix("userThread-");
        executor.setRejectedExecutionHandler((r, e) -> log.error("Task rejected: {}", r));
        executor.initialize();
        log.info("TaskExecutor initialized with corePoolSize=4");
        return executor;
    }
}
