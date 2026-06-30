package dev.sorokin.service;

import dev.sorokin.config.TaskDispatcherProperties;
import dev.sorokin.dao.TaskJpaRepository;
import dev.sorokin.domain.TaskEntity;
import dev.sorokin.domain.TaskResult;
import dev.sorokin.domain.TaskStatus;
import dev.sorokin.domain.TaskStep;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;

@Service
@AllArgsConstructor
@Slf4j
public class TaskDispatcher {

    private final TaskProcessor taskProcessor;
    private final AsyncTaskExecutor taskDispatcherAsyncExecutor;
    private final TaskJpaRepository taskRepository;
    private final Clock clock;
    private final TaskDispatcherProperties properties;

    @Transactional
    public void dispatch(TaskEntity task) {
        try {
            CompletableFuture
                    .supplyAsync(() -> taskProcessor.processTask(task), taskDispatcherAsyncExecutor)
                    .thenAccept(result -> handleTaskExecuted(task, result))
                    .exceptionally(ex -> handleExceptionInTaskHappened(task, ex));
        } catch (RejectedExecutionException ex) {
            log.warn("Task dispatch rejected (pool full or shutting down): taskId={}", task.getId(), ex);
            rescheduleAfterPoolReject(task);
        }
    }

    private void rescheduleAfterPoolReject(TaskEntity task) {
        var nextAttemptAt = Instant.now(clock).plus(properties.getPoolRejectRetryDelay());
        taskRepository.save(task.toBuilder()
                .status(TaskStatus.FAILED_RETRYABLE)
                .step(task.getStep())
                .nextAttemptAt(nextAttemptAt)
                .build());
    }

    private Void handleExceptionInTaskHappened(
            TaskEntity task,
            Throwable ex
    ) {
        log.error("Task failed with unexpected exception: taskId={}, step={}", task.getId(), task.getStep(), ex);
        scheduleTaskRetry(task, task.getStep());
        return null;
    }

    private void handleTaskExecuted(
            TaskEntity task,
            TaskProcessResult taskProcessResult
    ) {
        var taskStep = taskProcessResult.step();
        log.info("Task executed: taskId={}, status={}", task.getId(), taskProcessResult.status());
        switch (taskProcessResult.status()) {
            case SUCCESS -> handleTaskSucceeded(task, taskStep);
            case FAILED_RETRYABLE -> scheduleTaskRetry(task, taskStep);
            case FAILED_NON_RETRYABLE -> handleTaskFailed(task, taskStep);
        }
        log.info("Success processed execution status: taskId={}, status={}", task.getId(), taskProcessResult.status());
    }

    private void handleTaskFailed(TaskEntity task, TaskStep taskStep) {
        taskRepository.save(task.toBuilder()
                .status(TaskStatus.FAILED_NON_RETRYABLE)
                .step(taskStep)
                .taskResult(TaskResult.FAILURE)
                .nextAttemptAt(null)
                .build());
    }

    private void handleTaskSucceeded(TaskEntity task, TaskStep taskStep) {
        taskRepository.save(task.toBuilder()
                .status(TaskStatus.SUCCEEDED)
                .step(taskStep)
                .taskResult(TaskResult.SUCCESS)
                .nextAttemptAt(null)
                .build());
    }

    private void scheduleTaskRetry(TaskEntity task, TaskStep taskStep) {
        log.info("Scheduling default retry for taskId={}", task.getId());

        if (task.getAttempts() >= properties.getMaxAttempts()) {
            log.error("Maximum number of retries reached: taskId={}", task.getId());
            taskRepository.save(task.toBuilder()
                    .status(TaskStatus.FAILED_NON_RETRYABLE)
                    .step(taskStep)
                    .nextAttemptAt(null)
                    .build());
            return;
        }

        var nextAttemptAt = Instant.now(clock).plus(properties.getRetryDelay());
        taskRepository.save(task.toBuilder()
                .status(TaskStatus.FAILED_RETRYABLE)
                .step(taskStep)
                .nextAttemptAt(nextAttemptAt)
                .build());
    }

}
