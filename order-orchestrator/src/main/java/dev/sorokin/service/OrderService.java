package dev.sorokin.service;

import dev.sorokin.api.dto.OrderCreateRequestDto;
import dev.sorokin.dao.OrderJpaRepository;
import dev.sorokin.dao.TaskJpaRepository;
import dev.sorokin.domain.OrderEntity;
import dev.sorokin.domain.PaymentStatus;
import dev.sorokin.domain.TaskEntity;
import dev.sorokin.domain.TaskStatus;
import dev.sorokin.domain.TaskStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderJpaRepository orderRepository;
    private final TaskJpaRepository taskRepository;

    @Transactional
    public OrderEntity createOrder(
            OrderCreateRequestDto requestDto
    ) {
        var newOrder = OrderEntity.builder()
                .address(requestDto.address())
                .clientEstimate(requestDto.clientEstimate())
                .paymentStatus(PaymentStatus.NEW)
                .build();
        var createdOrder = orderRepository.save(newOrder);

        var newTask = TaskEntity.builder()
                .orderId(createdOrder.getId())
                .attempts(0)
                .step(TaskStep.AUTH)
                .taskStatus(TaskStatus.NEW)
                .build();
        taskRepository.save(newTask);

        return createdOrder;
    }

    public Optional<OrderEntity> findOrder(UUID id) {
        return orderRepository.findById(id);
    }
}
