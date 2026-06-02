package com.afetch.web.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiChatRequest(
        @NotBlank @Size(max = 4000) String message,
        Long conversationId
) {}
