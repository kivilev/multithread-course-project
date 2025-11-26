package dev.sorokin.domain;

import dev.sorokin.api.OrderCreateRequestDto;
import dev.sorokin.api.warehouse.CalculatePricingRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    public OrderEntity createOrder(
            OrderCreateRequestDto requestDto
    ) {
        return OrderEntity.builder()
                .address(requestDto.address())
                .build();
    }
}
