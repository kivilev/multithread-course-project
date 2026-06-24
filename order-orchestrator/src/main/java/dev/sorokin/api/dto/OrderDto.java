package dev.sorokin.api.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record OrderDto(
        UUID id,
        String address,
        Long customerId,
        String paymentStatus,
        BigDecimal clientEstimate,
        BigDecimal finalAmount,
        BigDecimal authorizedAmount,
        BigDecimal capturedAmount,
        String failureReason,
        String failureCode
) {
}
