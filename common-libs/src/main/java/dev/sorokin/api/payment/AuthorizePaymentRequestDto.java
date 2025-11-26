package dev.sorokin.api.payment;

import java.math.BigDecimal;

public record AuthorizePaymentRequestDto(
    Long customerId,
    BigDecimal amount
) { }
