package dev.sorokin.api;

import dev.sorokin.domain.OrderEntity;
import dev.sorokin.domain.OrderService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(
            @RequestBody OrderCreateRequestDto orderCreateRequestDto
    ) {
        var created = orderService.createOrder(orderCreateRequestDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(mapEntityToDto(created));
    }

    private OrderDto mapEntityToDto(OrderEntity order) {
        return OrderDto.builder()
                .address(order.getAddress())
                .build();
    }
}
