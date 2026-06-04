package com.afetch.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import com.afetch.domain.entity.AiConversation;
import com.afetch.domain.entity.AiMessage;
import com.afetch.domain.entity.User;
import com.afetch.domain.enums.AiMessageRole;
import com.afetch.integration.OpenRouterClient;
import com.afetch.repository.AiConversationRepository;
import com.afetch.repository.UserRepository;
import com.afetch.web.dto.ai.AiChatRequest;
import com.afetch.web.dto.ai.AiChatResponse;
import com.afetch.web.dto.ai.AiConversationDetails;
import com.afetch.web.dto.ai.AiConversationSummary;
import com.afetch.web.dto.ai.AiMessageDto;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private static final String SYSTEM_PROMPT = """
            You are the AI assistant for 'a fetch', a social platform for students at \
            Yerevan State College of Informatics (YSCI). Help users write and debug code,
            explain programming concepts, and answer questions about YSCI: departments,
            admission, campus life, and informatics education in Armenia.

            Speak like a professional programmer. Use English, Russian, or Armenian depending
            on the user's request, and support all three languages. If the user asks about YSCI,
            provide a friendly, accurate overview of the college, programs, and student life.
            """;

    private static final int MAX_HISTORY_MESSAGES = 20;

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

        AiConversation conversation = getOrCreateConversation(userId, request.conversationId(), user);

        AiMessage userMsg = new AiMessage();
        userMsg.setConversation(conversation);
        userMsg.setRole(AiMessageRole.USER);
        userMsg.setContent(request.message());
        conversation.getMessages().add(userMsg);

        List<Map<String, String>> payload = buildChatPayload(conversation);

        String reply;
        try {
            reply = openRouterClient.chat(payload);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (WebClientResponseException e) {
            log.warn("OpenRouter request failed, returning fallback assistant response: {}", e.getMessage());
            reply = "The AI provider is currently unavailable. I can still help with basic app guidance, but full model responses are temporarily offline.";
        } catch (IllegalStateException e) {
            log.warn("OpenRouter is not configured or available, returning fallback assistant response: {}", e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("OpenRouter API key is not configured")) {
                reply = "AI is in local fallback mode because the OpenRouter API key is not configured. Add OPENROUTER_API_KEY to enable full assistant answers.";
            } else {
                reply = "AI is in local fallback mode because the OpenRouter provider is unavailable. Add OPENROUTER_API_KEY and ensure the OpenRouter service is reachable.";
            }
        } catch (Exception e) {
            log.warn("Unexpected AI provider failure, returning fallback assistant response", e);
            reply = "The AI service hit a temporary error. Please try again in a moment.";
        }

        AiMessage assistantMsg = new AiMessage();
        assistantMsg.setConversation(conversation);
        assistantMsg.setRole(AiMessageRole.ASSISTANT);
        assistantMsg.setContent(reply);
        conversation.getMessages().add(assistantMsg);
        conversation.setLastActivity(java.time.Instant.now());

        // If conversation has no title yet and there is user content, generate a concise title
        if ((conversation.getTitle() == null || conversation.getTitle().isBlank()) && conversation.getMessages().stream().anyMatch(m -> m.getRole() == AiMessageRole.USER)) {
            String generated = generateTitleFromConversation(conversation);
            if (generated != null && !generated.isBlank()) {
                conversation.setTitle(generated);
            }
        }

        conversationRepository.save(conversation);
        return new AiChatResponse(reply, conversation.getId());
    }

    @Transactional(readOnly = true)
    public List<AiConversationSummary> listConversations(Long userId) {
        return conversationRepository.findByUserIdOrderByLastActivityDesc(userId).stream()
                .map(c -> new AiConversationSummary(c.getId(), c.getCreatedAt(), c.getMessages().size(), c.getTitle()))
                .toList();
    }

    @Transactional(readOnly = true)
    public AiConversationDetails getConversation(Long userId, Long conversationId) {
        AiConversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        List<AiMessageDto> messages = conversation.getMessages().stream()
                .map(m -> new AiMessageDto(m.getId(), m.getRole().name().toLowerCase(), m.getContent(), m.getCreatedAt()))
                .toList();

        return new AiConversationDetails(conversation.getId(), conversation.getCreatedAt(), messages, conversation.getTitle());
    }

    @Transactional
    public AiConversationSummary createConversation(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        AiConversation conv = createConversation(user);
        return new AiConversationSummary(conv.getId(), conv.getCreatedAt(), conv.getMessages().size(), conv.getTitle());
    }

    private String generateTitleFromConversation(AiConversation conversation) {
        // pick the first user message content and produce a short title
        return conversation.getMessages().stream()
                .filter(m -> m.getRole() == AiMessageRole.USER)
                .findFirst()
                .map(m -> {
                    String text = m.getContent() == null ? "" : m.getContent().trim();
                    if (text.isEmpty()) return null;
                    // simple heuristic: take first 6 words, remove punctuation
                    String cleaned = text.replaceAll("[\\p{Punct}&&[^'-]]+", " ").replaceAll("\\s+", " ").trim();
                    String[] parts = cleaned.split(" ");
                    int n = Math.min(6, parts.length);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < n; i++) {
                        String w = parts[i];
                        if (w.length() == 0) continue;
                        sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
                        if (i < n - 1) sb.append(' ');
                    }
                    String title = sb.toString();
                    if (title.length() > 60) title = title.substring(0, 57) + "...";
                    return title;
                })
                .orElse(null);
    }

    private AiConversation getOrCreateConversation(Long userId, Long conversationId, User user) {
        if (conversationId != null) {
            return conversationRepository.findByIdAndUserId(conversationId, userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
        }

        return createConversation(user);
    }

    private AiConversation createConversation(User user) {
        AiConversation conversation = new AiConversation();
        conversation.setUser(user);
        return conversationRepository.save(conversation);
    }

    private List<Map<String, String>> buildChatPayload(AiConversation conversation) {
        List<Map<String, String>> payload = new ArrayList<>();
        payload.add(Map.of("role", "system", "content", SYSTEM_PROMPT));

        int startIndex = Math.max(0, conversation.getMessages().size() - MAX_HISTORY_MESSAGES);
        for (int i = startIndex; i < conversation.getMessages().size(); i++) {
            AiMessage msg = conversation.getMessages().get(i);
            if (msg.getRole() == AiMessageRole.SYSTEM) {
                continue;
            }
            payload.add(Map.of("role", msg.getRole().name().toLowerCase(), "content", msg.getContent()));
        }
        return payload;
    }

    private void checkRateLimit(Long userId) {
        try {
            String key = "ai:rate:" + userId;
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1) {
                redis.expire(key, Duration.ofHours(1));
            }
            if (count != null && count > 30) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "AI rate limit exceeded");
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis unavailable for AI rate limit; skipping limit check for user {}", userId);
        }
    }
}
