package dev.sorokin.api.dto;

import dev.sorokin.domain.PaymentStatus;

import java.math.BigDecimal;

public record OrderCreateRequestDto(
        String address,
        BigDecimal clientEstimate,
        Long    customerId
) {
}
