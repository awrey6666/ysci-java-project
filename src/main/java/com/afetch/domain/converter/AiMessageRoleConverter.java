package com.afetch.domain.converter;

import com.afetch.domain.enums.AiMessageRole;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AiMessageRoleConverter implements AttributeConverter<AiMessageRole, String> {

    @Override
    public String convertToDatabaseColumn(AiMessageRole attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public AiMessageRole convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return AiMessageRole.valueOf(dbData.trim().toUpperCase());
    }
}
