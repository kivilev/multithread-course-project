package dev.sorokin.service.converter;

import dev.sorokin.domain.TaskStep;
import dev.sorokin.utils.EnumUtils;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class TaskStepConverter implements AttributeConverter<TaskStep, Integer> {

    @Override
    public Integer convertToDatabaseColumn(TaskStep statusEnum) {
        return statusEnum == null
                ? null
                : statusEnum.getId();
    }

    @Override
    public TaskStep convertToEntityAttribute(Integer intCode) {
        return intCode == null
                ? null
                : EnumUtils.fromCode(TaskStep.class, intCode);
    }
}
