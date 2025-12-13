package ru.mentee.power.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.Period;

/**
 * Конвертер для хранения Period как строки.
 */
@Converter(autoApply = true)
public class PeriodStringConverter implements AttributeConverter<Period, String> {

    @Override
    public String convertToDatabaseColumn(Period period) {
        return period == null ? null : period.toString();
    }

    @Override
    public Period convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Period.parse(dbData);
    }
}
