package dev.sorokin.api.dto;

import dev.sorokin.domain.PaymentStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record OrderDto(
        UUID id,
        String address,
        String paymentStatus,
        BigDecimal clientEstimate,
        BigDecimal finalAmount,
        BigDecimal authorizedAmount,
        BigDecimal capturedAmount,
        String failureReason,
        String failureCode
) {
}
