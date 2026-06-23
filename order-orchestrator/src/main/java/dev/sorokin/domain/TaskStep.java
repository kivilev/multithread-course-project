package dev.sorokin.domain;

import dev.sorokin.utils.EnumUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TaskStep implements EnumUtils.IntEnum {
    AUTH(0),
    REPRICE(1),
    CAPTURE(2);

    private final int id;

    @Override
    public int getCode() {
        return id;
    }
}
