package com.afetch.web.dto.ai;

import java.time.Instant;

public record AiMessageDto(
        Long id,
        String role,
        String content,
        Instant createdAt
) {
}
