package dev.sorokin.service;

import dev.sorokin.api.payment.AuthorizationStatus;
import dev.sorokin.api.payment.AuthorizePaymentRequestDto;
import dev.sorokin.api.payment.AuthorizePaymentResponseDto;
import dev.sorokin.api.payment.CapturePaymentRequestDto;
import dev.sorokin.api.payment.CapturePaymentResponseDto;
import dev.sorokin.api.payment.CaptureStatus;
import dev.sorokin.api.warehouse.CalculatePricingRequestDto;
import dev.sorokin.api.warehouse.CalculatePricingResponseDto;
import dev.sorokin.dao.OrderJpaRepository;
import dev.sorokin.dao.TaskJpaRepository;
import dev.sorokin.domain.OrderEntity;
import dev.sorokin.domain.PaymentStatus;
import dev.sorokin.domain.TaskEntity;
import dev.sorokin.domain.TaskExecutionStatus;
import dev.sorokin.domain.TaskStep;
import dev.sorokin.external.StubHttpClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
@AllArgsConstructor
public class TaskProcessor {

    private final OrderJpaRepository orderRepository;
    private final TaskJpaRepository taskRepository;
    private final StubHttpClient stubHttpClient;
    private final TransactionTemplate txTemplate;

    public TaskProcessResult processTask(TaskEntity task) {
        var orderId = task.getOrderId();
        var orderOptional = orderRepository.findById(orderId);
        if (orderOptional.isEmpty()) {
            log.error("Order not found: id={}", orderId);
            return new TaskProcessResult(TaskExecutionStatus.FAILED_NON_RETRYABLE, task.getStep());
        }
        var order = orderOptional.get();

        log.info("Processing task from step {}: taskId={}, orderId={}", task.getStep(), task.getId(), orderId);

        if (shouldRunAuth(task.getStep())) {
            var authResult = executeAuth(task, order);
            if (authResult != null) {
                return authResult;
            }
        } else {
            log.info("Skipping AUTH step, already completed: taskId={}, orderId={}", task.getId(), orderId);
        }

        if (shouldRunReprice(task.getStep())) {
            var repriceResult = executeReprice(task, order);
            if (repriceResult != null) {
                return repriceResult;
            }
        } else {
            log.info("Skipping REPRICE step, already completed: taskId={}, orderId={}", task.getId(), orderId);
        }

        return executeCapture(task, order);
    }

    private boolean shouldRunAuth(TaskStep step) {
        return step == TaskStep.NEW || step == TaskStep.AUTH;
    }

    private boolean shouldRunReprice(TaskStep step) {
        return step == TaskStep.REPRICE;
    }

    private TaskProcessResult executeAuth(TaskEntity task, OrderEntity order) {
        var orderId = order.getId();
        AuthorizePaymentResponseDto authorizePaymentResponse = stubHttpClient.paymentAuthorize(
                mapToAuthorizePaymentRequest(task, order));
        if (AuthorizationStatus.DECLINED.equals(authorizePaymentResponse.status())) {
            log.warn("Payment wasn't authorized. orderId:{}, taskId:{}, message:{}", orderId, task.getId(), authorizePaymentResponse.message());
            return new TaskProcessResult(handleAuthorizePaymentRejected(order, authorizePaymentResponse), TaskStep.AUTH);
        }
        persistInTransaction(() -> {
            order.setAuthorizedAmount(authorizePaymentResponse.authorizedAmount());
            order.setPaymentStatus(PaymentStatus.AUTHORIZED);
            orderRepository.save(order);
            task.setStep(TaskStep.REPRICE);
            taskRepository.save(task);
        });
        log.info("Payment was authorized. orderId:{}, taskId:{}", orderId, task.getId());
        return null;
    }

    private TaskProcessResult executeReprice(TaskEntity task, OrderEntity order) {
        var orderId = order.getId();
        if (order.getAuthorizedAmount() == null) {
            log.warn("REPRICE step requested but order is not authorized, restarting from AUTH: taskId={}, orderId={}",
                    task.getId(), orderId);
            advanceTaskStep(task, TaskStep.NEW);
            var authResult = executeAuth(task, order);
            if (authResult != null) {
                return authResult;
            }
        }

        CalculatePricingResponseDto calculateWarehouse = stubHttpClient.calculateWarehousePrice(mapToCalculatePricingRequest(order));
        var isWarehousePriceBigger = calculateWarehouse.finalAmount().compareTo(order.getAuthorizedAmount()) > 0;
        if (isWarehousePriceBigger) {
            log.warn("Order price was changed. orderId:{}, taskId:{}, authPrice:{}, warehousePrice:{}, reason:{}", orderId, task.getId(),
                    order.getAuthorizedAmount(), calculateWarehouse.finalAmount(), calculateWarehouse.reason());
            return new TaskProcessResult(handleWarehousePriceBiggerRejected(order, calculateWarehouse), TaskStep.REPRICE);
        }
        persistInTransaction(() -> {
            order.setFinalAmount(calculateWarehouse.finalAmount());
            orderRepository.save(order);
            task.setStep(TaskStep.CAPTURE);
            taskRepository.save(task);
        });
        log.info("Order price calculated, proceeding to capture. orderId:{}, taskId:{}", orderId, task.getId());
        return null;
    }

    private TaskProcessResult executeCapture(TaskEntity task, OrderEntity order) {
        var orderId = order.getId();
        if (order.getFinalAmount() == null) {
            log.warn("CAPTURE step requested but final amount is missing, restarting from REPRICE: taskId={}, orderId={}",
                    task.getId(), orderId);
            advanceTaskStep(task, TaskStep.REPRICE);
            var repriceResult = executeReprice(task, order);
            if (repriceResult != null) {
                return repriceResult;
            }
        }

        CapturePaymentResponseDto paymentCaptureResponse = stubHttpClient.paymentCapture(
                mapToCapturePaymentRequest(task, order));
        if (CaptureStatus.FAILED.equals(paymentCaptureResponse.status())) {
            log.warn("Payment wasn't captured. orderId:{}, taskId:{}, message:{}", orderId, task.getId(),
                    paymentCaptureResponse.message());
            return new TaskProcessResult(handlePaymentCaptureRejected(order, paymentCaptureResponse), TaskStep.CAPTURE);
        }
        order.setCapturedAmount(paymentCaptureResponse.capturedAmount());
        order.setPaymentStatus(PaymentStatus.SUCCEED_PAID);
        orderRepository.save(order);
        log.info("Payment was captured. orderId:{}, taskId:{}", orderId, task.getId());

        return new TaskProcessResult(TaskExecutionStatus.SUCCESS, TaskStep.CAPTURE);
    }

    private void advanceTaskStep(TaskEntity task, TaskStep nextStep) {
        persistInTransaction(() -> {
            task.setStep(nextStep);
            taskRepository.save(task);
        });
    }

    private void persistInTransaction(Runnable action) {
        txTemplate.executeWithoutResult(status -> action.run());
    }

    private TaskExecutionStatus handlePaymentCaptureRejected(OrderEntity order, CapturePaymentResponseDto paymentCaptureResponse) {
        order.setPaymentStatus(PaymentStatus.CAPTURE_FAILED);
        order.setCapturedAmount(paymentCaptureResponse.capturedAmount());
        order.setFailureReason(paymentCaptureResponse.message());
        orderRepository.save(order);
        return TaskExecutionStatus.FAILED_NON_RETRYABLE;
    }

    private TaskExecutionStatus handleWarehousePriceBiggerRejected(OrderEntity order, CalculatePricingResponseDto calculateWarehouse) {
        order.setPaymentStatus(PaymentStatus.PRICE_CHANGED_FAILED);
        order.setFailureReason(calculateWarehouse.reason());
        order.setFinalAmount(calculateWarehouse.finalAmount());
        orderRepository.save(order);
        return TaskExecutionStatus.FAILED_NON_RETRYABLE;
    }

    private TaskExecutionStatus handleAuthorizePaymentRejected(OrderEntity order, AuthorizePaymentResponseDto authorizePaymentResponse) {
        order.setPaymentStatus(PaymentStatus.AUTHORIZATION_FAILED);
        order.setFailureReason(authorizePaymentResponse.message());
        order.setAuthorizedAmount(authorizePaymentResponse.authorizedAmount());
        orderRepository.save(order);
        return TaskExecutionStatus.FAILED_NON_RETRYABLE;
    }

    private AuthorizePaymentRequestDto mapToAuthorizePaymentRequest(TaskEntity task, OrderEntity order) {
        return new AuthorizePaymentRequestDto(
                order.getCustomerId(),
                order.getClientEstimate(),
                idempotencyKey(task, "auth")
        );
    }

    private CalculatePricingRequestDto mapToCalculatePricingRequest(OrderEntity order) {
        return new CalculatePricingRequestDto(order.getId());
    }

    private CapturePaymentRequestDto mapToCapturePaymentRequest(TaskEntity task, OrderEntity order) {
        return new CapturePaymentRequestDto(
                order.getFinalAmount(),
                order.getCustomerId(),
                idempotencyKey(task, "capture")
        );
    }

    private String idempotencyKey(TaskEntity task, String operation) {
        return task.getId() + ":" + operation;
    }
}
