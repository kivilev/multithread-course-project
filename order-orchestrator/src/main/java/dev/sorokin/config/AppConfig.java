package dev.sorokin.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(value = {TaskDispatcherProperties.class, TaskExecutionPollerProperties.class})
@EnableScheduling
public class AppConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    AsyncTaskExecutor taskDispatcherAsyncExecutor(TaskDispatcherProperties props) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(props.getThreadPoolSize());
        executor.setMaxPoolSize(props.getThreadPoolSize());
        executor.setQueueCapacity(props.getQueueSize());
        executor.setThreadNamePrefix("task-disp-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(props.getAwaitTerminationSeconds());
        executor.initialize();
        return executor;
    }
}
