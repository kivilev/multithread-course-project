package dev.sorokin.domain;

import dev.sorokin.utils.EnumUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TaskStep implements EnumUtils.IntEnum {
    NEW(0),
    AUTH(1),
    REPRICE(2),
    CAPTURE(3);

    private final int id;

    @Override
    public int getCode() {
        return id;
    }
}
