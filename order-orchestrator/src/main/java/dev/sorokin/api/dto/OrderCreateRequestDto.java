package dev.sorokin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OrderCreateRequestDto(
        @NotBlank
        String address,
        @NotNull
        @Positive
        BigDecimal clientEstimate,
        @NotNull
        Long customerId
) {
}
