package com.afetch.web.dto.chat;

import java.time.Instant;

public record ChatRoomResponse(
        Long id,
        String type,
        String slug,
        String name,
        long onlineCount,
        String lastMessagePreview,
        Instant lastMessageAt,
        int unreadCount,
        Long dmPartnerId,
        String dmPartnerUsername,
        String dmPartnerDisplayName,
        String dmPartnerAvatarUrl
) {}
