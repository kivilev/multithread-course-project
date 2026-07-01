package dev.sorokin.domain;

import dev.sorokin.utils.EnumUtils;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum TaskStatus implements EnumUtils.IntEnum {
    NEW(0),
    IN_PROGRESS(1),
    SUCCEEDED(2),
    FAILED_RETRYABLE(3),
    FAILED_NON_RETRYABLE(4);

    private final int id;

    @Override
    public int getCode() {
        return id;
    }
}
