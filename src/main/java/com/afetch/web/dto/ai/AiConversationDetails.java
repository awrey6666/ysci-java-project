package com.afetch.web.dto.ai;

import java.time.Instant;
import java.util.List;

public record AiConversationDetails(
        Long id,
        Instant createdAt,
        List<AiMessageDto> messages,
        String title
) {
}
