package dev.sorokin.api.payment;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Запрос на списание (capture) по ранее авторизованной карте
 */
public record CapturePaymentRequestDto(
        BigDecimal captureAmount,
        UUID customerId
) {}
