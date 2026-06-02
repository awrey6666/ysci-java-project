package com.afetch.domain.converter;

import com.afetch.domain.enums.ChatRoomType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ChatRoomTypeConverter implements AttributeConverter<ChatRoomType, String> {

    @Override
    public String convertToDatabaseColumn(ChatRoomType attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public ChatRoomType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return ChatRoomType.valueOf(dbData.trim().toUpperCase());
    }
}
