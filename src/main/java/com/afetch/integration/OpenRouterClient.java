package com.afetch.integration;

import com.afetch.config.AfetchProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
public class OpenRouterClient {

    private final WebClient webClient;
    private final String model;

    public OpenRouterClient(AfetchProperties properties) {
        this.model = properties.getOpenrouter().getModel();
        this.webClient = WebClient.builder()
                .baseUrl(properties.getOpenrouter().getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getOpenrouter().getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @SuppressWarnings("unchecked")
    public String chat(List<Map<String, String>> messages) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", messages
        );

        Map<String, Object> response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) {
            throw new IllegalStateException("Empty response from OpenRouter");
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }
}
