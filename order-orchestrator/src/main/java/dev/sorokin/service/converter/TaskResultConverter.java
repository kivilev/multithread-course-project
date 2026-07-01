package dev.sorokin.service.converter;

import dev.sorokin.domain.TaskResult;
import dev.sorokin.utils.EnumUtils;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class TaskResultConverter implements AttributeConverter<TaskResult, Integer> {

    @Override
    public Integer convertToDatabaseColumn(TaskResult statusEnum) {
        return statusEnum == null
                ? null
                : statusEnum.getCode();
    }

    @Override
    public TaskResult convertToEntityAttribute(Integer intCode) {
        return intCode == null
                ? null
                : EnumUtils.fromCode(TaskResult.class, intCode);
    }
}
