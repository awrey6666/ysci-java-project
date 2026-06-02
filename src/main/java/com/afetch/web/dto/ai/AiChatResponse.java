package com.afetch.web.dto.ai;

public record AiChatResponse(
        String reply,
        Long conversationId
) {}
