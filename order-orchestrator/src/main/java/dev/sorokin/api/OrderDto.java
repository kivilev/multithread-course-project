package dev.sorokin.api;

import lombok.Builder;

@Builder
public record OrderDto(
        String address // todo остальные поля
) { }
