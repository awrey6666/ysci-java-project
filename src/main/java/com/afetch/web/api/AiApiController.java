package com.afetch.web.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.afetch.security.SecurityUtils;
import com.afetch.service.AiService;
import com.afetch.web.dto.ai.AiChatRequest;
import com.afetch.web.dto.ai.AiChatResponse;
import com.afetch.web.dto.ai.AiConversationDetails;
import com.afetch.web.dto.ai.AiConversationSummary;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ai")
public class AiApiController {

    private final AiService aiService;

    public AiApiController(AiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/conversations")
    public List<AiConversationSummary> listConversations() {
        return aiService.listConversations(SecurityUtils.currentUserId());
    }

    @PostMapping("/conversations")
    public AiConversationSummary createConversation() {
        return aiService.createConversation(SecurityUtils.currentUserId());
    }

    @GetMapping("/conversations/{id}")
    public AiConversationDetails getConversation(@PathVariable Long id) {
        return aiService.getConversation(SecurityUtils.currentUserId(), id);
    }

    @PostMapping("/chat")
    public AiChatResponse chat(@Valid @RequestBody AiChatRequest request) {
        return aiService.chat(SecurityUtils.currentUserId(), request);
    }
}
