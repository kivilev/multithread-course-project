package dev.sorokin.domain;


import dev.sorokin.utils.EnumUtils;

public enum TaskExecutionStatus implements EnumUtils.StringEnum {

    SUCCESS("SUCCESS"),
    FAILED_RETRYABLE("FAILED_RETRYABLE"),
    FAILED_NON_RETRYABLE("FAILED_NON_RETRYABLE"),
    ;

    private final String value;

    TaskExecutionStatus(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
