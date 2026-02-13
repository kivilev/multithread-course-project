package dev.sorokin.api.payment;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Запрос на авторизацию карты на указанную сумму
 */
public record AuthorizePaymentRequestDto(
    UUID customerId,
    BigDecimal amount
) { }
