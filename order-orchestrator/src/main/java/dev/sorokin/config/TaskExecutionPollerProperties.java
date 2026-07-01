package dev.sorokin.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "task-execution.poller")
public class TaskExecutionPollerProperties {
    private int pollIntervalMs;

    private int batchSize;

    private Duration inProgressLeaseDuration;
}
