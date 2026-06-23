package dev.sorokin.domain;

import dev.sorokin.utils.EnumUtils;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum TaskResult implements EnumUtils.IntEnum {
    SUCCESS(1),
    FAILURE(0);

    private final int id;

    @Override
    public int getCode() {
        return id;
    }
}
