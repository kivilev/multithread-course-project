package dev.sorokin.service;

import dev.sorokin.config.TaskExecutionPollerProperties;
import dev.sorokin.dao.TaskJpaRepository;
import dev.sorokin.domain.TaskEntity;
import dev.sorokin.domain.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class TaskPoller {

    private final TaskJpaRepository taskRepository;
    private final Clock clock;
    private final TransactionTemplate txTemplate;
    private final TaskDispatcher taskDispatcher;
    private final TaskExecutionPollerProperties taskExecutionPollerProperties;

    @Scheduled(fixedRateString = "${task-execution.poller.poll-interval-ms}")
    public void poll() {
        try {
            var tasks = pickTasksForProcessing();
            log.info("Tasks were picked. Count:{}, Ids:{}", tasks.size(), tasks.stream().map(TaskEntity::getId).toList());

            tasks.forEach(taskDispatcher::dispatch);
            log.info("Tasks were dispatched. Count:{}, Ids:{}", tasks.size(), tasks.stream().map(TaskEntity::getId).toList());
        } catch (Exception e) {
            log.error("Task poller got exception. Message:{}, stack:{}", e.getMessage(), e.getStackTrace());
        }
    }

    private List<TaskEntity> pickTasksForProcessing() {
        return txTemplate.execute(status -> {
            List<TaskEntity> tasks = taskRepository.pickAllUnprocessedTasksWithLocking(TaskStatus.NEW,
                    TaskStatus.FAILED_RETRYABLE,
                    TaskStatus.IN_PROGRESS,
                    Instant.now(clock),
                    taskExecutionPollerProperties.getBatchSize());

            var leaseExpiresAt = Instant.now(clock).plus(taskExecutionPollerProperties.getInProgressLeaseDuration());

            tasks.forEach(task -> {
                task.setStatus(TaskStatus.IN_PROGRESS);
                task.setAttempts(task.getAttempts() + 1);
                task.setNextAttemptAt(leaseExpiresAt);
            });

            taskRepository.saveAll(tasks);
            return tasks;
        });
    }

}
