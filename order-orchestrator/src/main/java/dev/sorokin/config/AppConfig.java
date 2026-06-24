package dev.sorokin.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableConfigurationProperties(value = {TaskDispatcherProperties.class, TaskExecutionPollerProperties.class})
@EnableScheduling
public class AppConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ExecutorService taskDispatcherThreadPool(TaskDispatcherProperties taskDispatcherProperties) {
        return Executors.newFixedThreadPool(
                taskDispatcherProperties.getThreadPoolSize(),
                Thread.ofPlatform()
                        .name("task-disp-", 0)
                        .factory()
        );
    }
}
