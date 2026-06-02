package com.afetch.web.dto.chat;

public record SendChatMessageRequest(
        Long roomId,
        String body,
        String imageUrl,
        Long parentId
) {}
