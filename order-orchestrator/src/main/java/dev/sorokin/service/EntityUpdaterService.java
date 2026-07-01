package dev.sorokin.service;

import dev.sorokin.dao.OrderJpaRepository;
import dev.sorokin.dao.TaskJpaRepository;
import dev.sorokin.domain.OrderEntity;
import dev.sorokin.domain.TaskEntity;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Service
@AllArgsConstructor
@Slf4j
public class EntityUpdaterService {

    private final TaskJpaRepository taskRepository;
    private final OrderJpaRepository orderRepository;
    private final TransactionTemplate txTemplate;

    public void updateTask(UUID taskId, Consumer<TaskEntity> mutator) {
        try {
            txTemplate.executeWithoutResult(status -> {
                var task = taskRepository.findById(taskId).orElseThrow();
                mutator.accept(task);
                taskRepository.save(task);
            });
        } catch (OptimisticLockingFailureException ex) {
            log.warn("Optimistic lock conflict updating task: taskId={}", taskId, ex);
            throw new StaleEntityException("task " + taskId);
        }
    }

    public void updateOrder(UUID orderId, Consumer<OrderEntity> mutator) {
        try {
            txTemplate.executeWithoutResult(status -> {
                var order = orderRepository.findById(orderId).orElseThrow();
                mutator.accept(order);
                orderRepository.save(order);
            });
        } catch (OptimisticLockingFailureException ex) {
            log.warn("Optimistic lock conflict updating order: orderId={}", orderId, ex);
            throw new StaleEntityException("order " + orderId);
        }
    }

    public void updateTaskAndOrder(UUID taskId, UUID orderId, BiConsumer<TaskEntity, OrderEntity> mutator) {
        try {
            txTemplate.executeWithoutResult(status -> {
                var task = taskRepository.findById(taskId).orElseThrow();
                var order = orderRepository.findById(orderId).orElseThrow();
                mutator.accept(task, order);
                orderRepository.save(order);
                taskRepository.save(task);
            });
        } catch (OptimisticLockingFailureException ex) {
            log.warn("Optimistic lock conflict updating task and order: taskId={}, orderId={}", taskId, orderId, ex);
            throw new StaleEntityException("task " + taskId + ", order " + orderId);
        }
    }
}
