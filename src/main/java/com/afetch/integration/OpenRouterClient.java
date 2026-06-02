package com.afetch.integration;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.afetch.config.AfetchProperties;

@Component
public class OpenRouterClient {

    private final WebClient webClient;
    private final String model;
    private final String apiKey;

    public OpenRouterClient(AfetchProperties properties) {
        this.apiKey = properties.getOpenrouter().getApiKey();
        this.model = properties.getOpenrouter().getModel();
        this.webClient = WebClient.builder()
                .baseUrl(properties.getOpenrouter().getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + (apiKey != null ? apiKey : ""))
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("HTTP-Referer", "https://afetch.local")
                .build();
    }

    @SuppressWarnings("unchecked")
    public String chat(List<Map<String, String>> messages) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException(
                    "OpenRouter API key is not configured. Check `afetch.openrouter.api-key` in application.yml (or provide env var mapped to it)."
            );
        }


        Map<String, Object> body = Map.of(
                "model", model,
                "messages", messages
        );

        try {
            Map<String, Object> response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                throw new IllegalStateException("Empty response from OpenRouter API");
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new IllegalStateException("No choices in OpenRouter response");
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null) {
                throw new IllegalStateException("No message in OpenRouter response");
            }

            Object content = message.get("content");
            if (content == null) {
                throw new IllegalStateException("No content in OpenRouter message");
            }

            return content.toString();
        } catch (WebClientResponseException e) {
            String errorMessage = String.format("OpenRouter API error: %d - %s", e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new IllegalStateException(errorMessage, e);
        } catch (Exception e) {
            if (e instanceof IllegalStateException) {
                throw e;
            }
            throw new IllegalStateException("Failed to communicate with OpenRouter API: " + e.getMessage(), e);
        }
    }
}
