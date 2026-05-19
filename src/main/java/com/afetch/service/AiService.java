package com.afetch.service;

import com.afetch.domain.entity.AiConversation;
import com.afetch.domain.entity.AiMessage;
import com.afetch.domain.entity.User;
import com.afetch.domain.enums.AiMessageRole;
import com.afetch.integration.OpenRouterClient;
import com.afetch.repository.AiConversationRepository;
import com.afetch.repository.UserRepository;
import com.afetch.web.dto.ai.AiChatRequest;
import com.afetch.web.dto.ai.AiChatResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    private static final String SYSTEM_PROMPT = """
            You are the AI assistant for 'a fetch', a social platform for students at \
            Yerevan State College of Informatics (YSCI). Help users write and debug code, \
            explain programming concepts, and answer questions about YSCI: departments, \
            admission, campus life, and informatics education in Armenia. Be concise and helpful.
            """;

    private final AiConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final OpenRouterClient openRouterClient;
    private final StringRedisTemplate redis;

    public AiService(AiConversationRepository conversationRepository,
                     UserRepository userRepository,
                     OpenRouterClient openRouterClient,
                     StringRedisTemplate redis) {
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.openRouterClient = openRouterClient;
        this.redis = redis;
    }

    @Transactional
    public AiChatResponse chat(Long userId, AiChatRequest request) {
        checkRateLimit(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        AiConversation conversation = conversationRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
                .orElseGet(() -> {
                    AiConversation c = new AiConversation();
                    c.setUser(user);
                    return conversationRepository.save(c);
                });

        AiMessage userMsg = new AiMessage();
        userMsg.setConversation(conversation);
        userMsg.setRole(AiMessageRole.USER);
        userMsg.setContent(request.message());
        conversation.getMessages().add(userMsg);

        List<Map<String, String>> payload = new ArrayList<>();
        payload.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        for (AiMessage msg : conversation.getMessages()) {
            payload.add(Map.of("role", msg.getRole().name().toLowerCase(), "content", msg.getContent()));
        }

        String reply;
        try {
            reply = openRouterClient.chat(payload);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI service unavailable");
        }

        AiMessage assistantMsg = new AiMessage();
        assistantMsg.setConversation(conversation);
        assistantMsg.setRole(AiMessageRole.ASSISTANT);
        assistantMsg.setContent(reply);
        conversation.getMessages().add(assistantMsg);

        conversationRepository.save(conversation);
        return new AiChatResponse(reply, conversation.getId());
    }

    private void checkRateLimit(Long userId) {
        String key = "ai:rate:" + userId;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) {
            redis.expire(key, Duration.ofHours(1));
        }
        if (count != null && count > 30) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "AI rate limit exceeded");
        }
    }
}
