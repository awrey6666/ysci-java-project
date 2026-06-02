package com.afetch.web.dto.ai;

import java.time.Instant;

public record AiConversationSummary(
        Long id,
        Instant createdAt,
        int messagesCount
) {
}
