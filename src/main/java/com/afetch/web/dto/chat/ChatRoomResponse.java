package com.afetch.web.dto.chat;

public record ChatRoomResponse(
        Long id,
        String type,
        String slug,
        String name,
        long onlineCount
) {}
