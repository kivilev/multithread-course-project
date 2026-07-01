package dev.sorokin.service;

import dev.sorokin.config.TaskDispatcherProperties;
import dev.sorokin.domain.TaskEntity;
import dev.sorokin.domain.TaskResult;
import dev.sorokin.domain.TaskStatus;
import dev.sorokin.domain.TaskStep;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;

@Service
@AllArgsConstructor
@Slf4j
public class TaskDispatcher {

    private final TaskProcessor taskProcessor;
    private final AsyncTaskExecutor taskDispatcherAsyncExecutor;
    private final EntityUpdaterService entityUpdaterService;
    private final Clock clock;
    private final TaskDispatcherProperties properties;

    public void dispatch(TaskEntity task) {
        try {
            CompletableFuture
                    .supplyAsync(() -> taskProcessor.processTask(task), taskDispatcherAsyncExecutor)
                    .thenAccept(result -> handleTaskExecuted(task.getId(), result))
                    .exceptionally(ex -> handleExceptionInTaskHappened(task, ex));
        } catch (RejectedExecutionException ex) {
            log.warn("Task dispatch rejected (pool full or shutting down): taskId={}", task.getId(), ex);
            rescheduleAfterPoolReject(task.getId());
        }
    }

    private void rescheduleAfterPoolReject(UUID taskId) {
        var nextAttemptAt = Instant.now(clock).plus(properties.getPoolRejectRetryDelay());
        entityUpdaterService.updateTask(taskId, task -> {
            task.setStatus(TaskStatus.FAILED_RETRYABLE);
            task.setNextAttemptAt(nextAttemptAt);
            task.setLockedUntil(null);
        });
    }

    private Void handleExceptionInTaskHappened(TaskEntity task, Throwable ex) {
        if (isStaleOrOptimisticLock(ex)) {
            log.warn("Stale task processing aborted: taskId={}", task.getId(), ex);
            return null;
        }
        log.error("Task failed with unexpected exception: taskId={}, step={}", task.getId(), task.getStep(), ex);
        scheduleTaskRetry(task.getId(), task.getStep());
        return null;
    }

    private void handleTaskExecuted(UUID taskId, TaskProcessResult taskProcessResult) {
        var taskStep = taskProcessResult.step();
        log.info("Task executed: taskId={}, status={}", taskId, taskProcessResult.status());
        switch (taskProcessResult.status()) {
            case SUCCESS -> handleTaskSucceeded(taskId, taskStep);
            case FAILED_RETRYABLE -> scheduleTaskRetry(taskId, taskStep);
            case FAILED_NON_RETRYABLE -> handleTaskFailed(taskId, taskStep);
        }
        log.info("Success processed execution status: taskId={}, status={}", taskId, taskProcessResult.status());
    }

    private void handleTaskFailed(UUID taskId, TaskStep taskStep) {
        entityUpdaterService.updateTask(taskId, task -> {
            task.setStatus(TaskStatus.FAILED_NON_RETRYABLE);
            task.setStep(taskStep);
            task.setTaskResult(TaskResult.FAILURE);
            task.setNextAttemptAt(null);
            task.setLockedUntil(null);
        });
    }

    private void handleTaskSucceeded(UUID taskId, TaskStep taskStep) {
        entityUpdaterService.updateTask(taskId, task -> {
            task.setStatus(TaskStatus.SUCCEEDED);
            task.setStep(taskStep);
            task.setTaskResult(TaskResult.SUCCESS);
            task.setNextAttemptAt(null);
            task.setLockedUntil(null);
        });
    }

    private void scheduleTaskRetry(UUID taskId, TaskStep taskStep) {
        log.info("Scheduling default retry for taskId={}", taskId);

        entityUpdaterService.updateTask(taskId, task -> {
            var nextAttempts = task.getAttempts() + 1;
            if (nextAttempts >= properties.getMaxAttempts()) {
                log.error("Maximum number of retries reached: taskId={}", taskId);
                task.setStatus(TaskStatus.FAILED_NON_RETRYABLE);
                task.setStep(taskStep);
                task.setAttempts(nextAttempts);
                task.setNextAttemptAt(null);
                task.setLockedUntil(null);
                return;
            }

            var nextAttemptAt = Instant.now(clock).plus(properties.getRetryDelay());
            task.setStatus(TaskStatus.FAILED_RETRYABLE);
            task.setStep(taskStep);
            task.setAttempts(nextAttempts);
            task.setNextAttemptAt(nextAttemptAt);
            task.setLockedUntil(null);
        });
    }

    private boolean isStaleOrOptimisticLock(Throwable ex) {
        var current = ex;
        while (current != null) {
            if (current instanceof StaleEntityException || current instanceof OptimisticLockingFailureException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
