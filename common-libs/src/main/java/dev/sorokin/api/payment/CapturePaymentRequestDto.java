package dev.sorokin.api.payment;

import java.math.BigDecimal;

public record CapturePaymentRequestDto(
        BigDecimal captureAmount,
        Long customerId
) {}
