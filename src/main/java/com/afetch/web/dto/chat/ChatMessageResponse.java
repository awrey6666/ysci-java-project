package com.afetch.web.dto.chat;

import java.time.Instant;
import java.util.List;

public record ChatMessageResponse(
        Long id,
        Long roomId,
        Long parentId,
        String body,
        String imageUrl,
        String senderUsername,
        Long senderId,
        List<String> mentionedUsernames,
        Instant createdAt
) {}
