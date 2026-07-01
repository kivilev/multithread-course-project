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
import dev.sorokin.domain.OrderEntity;
import dev.sorokin.domain.PaymentStatus;
import dev.sorokin.domain.TaskEntity;
import dev.sorokin.domain.TaskExecutionStatus;
import dev.sorokin.domain.TaskStep;
import dev.sorokin.external.StubHttpClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class TaskProcessor {

    private final OrderJpaRepository orderRepository;
    private final StubHttpClient stubHttpClient;
    private final EntityUpdaterService entityUpdaterService;

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
            return new TaskProcessResult(handleAuthorizePaymentRejected(task, order, authorizePaymentResponse), TaskStep.AUTH);
        }
        entityUpdaterService.updateTaskAndOrder(task.getId(), orderId, (freshTask, freshOrder) -> {
            freshOrder.setAuthorizedAmount(authorizePaymentResponse.authorizedAmount());
            freshOrder.setPaymentStatus(PaymentStatus.AUTHORIZED);
            freshTask.setStep(TaskStep.REPRICE);
        });
        task.setStep(TaskStep.REPRICE);
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
            return new TaskProcessResult(handleWarehousePriceBiggerRejected(task, order, calculateWarehouse), TaskStep.REPRICE);
        }
        entityUpdaterService.updateTaskAndOrder(task.getId(), orderId, (freshTask, freshOrder) -> {
            freshOrder.setFinalAmount(calculateWarehouse.finalAmount());
            freshTask.setStep(TaskStep.CAPTURE);
        });
        task.setStep(TaskStep.CAPTURE);
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
            return new TaskProcessResult(handlePaymentCaptureRejected(task, order, paymentCaptureResponse), TaskStep.CAPTURE);
        }
        entityUpdaterService.updateOrder(orderId, freshOrder -> {
            freshOrder.setCapturedAmount(paymentCaptureResponse.capturedAmount());
            freshOrder.setPaymentStatus(PaymentStatus.SUCCEED_PAID);
        });
        log.info("Payment was captured. orderId:{}, taskId:{}", orderId, task.getId());

        return new TaskProcessResult(TaskExecutionStatus.SUCCESS, TaskStep.CAPTURE);
    }

    private void advanceTaskStep(TaskEntity task, TaskStep nextStep) {
        entityUpdaterService.updateTask(task.getId(), freshTask -> freshTask.setStep(nextStep));
        task.setStep(nextStep);
    }

    private TaskExecutionStatus handlePaymentCaptureRejected(
            TaskEntity task,
            OrderEntity order,
            CapturePaymentResponseDto paymentCaptureResponse
    ) {
        entityUpdaterService.updateOrder(order.getId(), freshOrder -> {
            freshOrder.setPaymentStatus(PaymentStatus.CAPTURE_FAILED);
            freshOrder.setCapturedAmount(paymentCaptureResponse.capturedAmount());
            freshOrder.setFailureReason(paymentCaptureResponse.message());
        });
        return TaskExecutionStatus.FAILED_NON_RETRYABLE;
    }

    private TaskExecutionStatus handleWarehousePriceBiggerRejected(
            TaskEntity task,
            OrderEntity order,
            CalculatePricingResponseDto calculateWarehouse
    ) {
        entityUpdaterService.updateOrder(order.getId(), freshOrder -> {
            freshOrder.setPaymentStatus(PaymentStatus.PRICE_CHANGED_FAILED);
            freshOrder.setFailureReason(calculateWarehouse.reason());
            freshOrder.setFinalAmount(calculateWarehouse.finalAmount());
        });
        return TaskExecutionStatus.FAILED_NON_RETRYABLE;
    }

    private TaskExecutionStatus handleAuthorizePaymentRejected(
            TaskEntity task,
            OrderEntity order,
            AuthorizePaymentResponseDto authorizePaymentResponse
    ) {
        entityUpdaterService.updateOrder(order.getId(), freshOrder -> {
            freshOrder.setPaymentStatus(PaymentStatus.AUTHORIZATION_FAILED);
            freshOrder.setFailureReason(authorizePaymentResponse.message());
            freshOrder.setAuthorizedAmount(authorizePaymentResponse.authorizedAmount());
        });
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
