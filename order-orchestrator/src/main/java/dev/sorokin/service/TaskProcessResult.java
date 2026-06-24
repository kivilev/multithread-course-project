package dev.sorokin.service;

import dev.sorokin.domain.TaskExecutionStatus;
import dev.sorokin.domain.TaskStep;

public record TaskProcessResult(
        TaskExecutionStatus status,
        TaskStep step
) {
}
