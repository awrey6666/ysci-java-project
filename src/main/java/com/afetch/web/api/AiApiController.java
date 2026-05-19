package com.afetch.web.api;

import com.afetch.security.SecurityUtils;
import com.afetch.service.AiService;
import com.afetch.web.dto.ai.AiChatRequest;
import com.afetch.web.dto.ai.AiChatResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiApiController {

    private final AiService aiService;

    public AiApiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/chat")
    public AiChatResponse chat(@Valid @RequestBody AiChatRequest request) {
        return aiService.chat(SecurityUtils.currentUserId(), request);
    }
}
