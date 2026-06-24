package dev.sorokin.api;

import dev.sorokin.api.dto.OrderCreateRequestDto;
import dev.sorokin.api.dto.OrderDto;
import dev.sorokin.domain.OrderEntity;
import dev.sorokin.service.OrderService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(
            @RequestBody OrderCreateRequestDto orderCreateRequestDto
    ) {
        log.info("Received request to create order: request={}", orderCreateRequestDto);
        var created = orderService.createOrder(orderCreateRequestDto);
        log.info("Created order: created={}", created);
        return ResponseEntity
                .created(URI.create("/order/" + created.getId()))
                .body(mapEntityToDto(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrder(
            @PathVariable UUID id
    ) {
        log.info("Getting order with id {}", id);
        var foundOrder = orderService.findOrder(id);
        return foundOrder
                .map(this::mapEntityToDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private OrderDto mapEntityToDto(OrderEntity order) {
        return OrderDto.builder()
                .id(order.getId())
                .address(order.getAddress())
                .customerId(order.getCustomerId())
                .paymentStatus(order.getPaymentStatus().name())
                .clientEstimate(order.getClientEstimate())
                .capturedAmount(order.getCapturedAmount())
                .finalAmount(order.getFinalAmount())
                .authorizedAmount(order.getAuthorizedAmount())
                .failureReason(order.getFailureReason())
                .failureCode(order.getFailureCode())
                .build();
    }
}
