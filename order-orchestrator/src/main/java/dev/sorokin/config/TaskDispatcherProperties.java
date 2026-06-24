package dev.sorokin.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "task-execution.dispatcher")
public class TaskDispatcherProperties {
    private Duration retryDelay;

    private int threadPoolSize;

    private int maxAttempts;
}
