package com.afetch.integration;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.afetch.config.AfetchProperties;

@Component
public class OpenRouterClient {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterClient.class);

    private final WebClient webClient;
    private final String model;
    private final String apiKey;

    public OpenRouterClient(AfetchProperties properties) {
        String propertyApiKey = normalize(properties.getOpenrouter().getApiKey());
        String envApiKey = normalize(System.getenv("OPENROUTER_API_KEY"));
        boolean propertyApiKeyPresent = !propertyApiKey.isBlank();
        boolean envApiKeyPresent = !envApiKey.isBlank();
        this.apiKey = propertyApiKeyPresent ? propertyApiKey : envApiKey;

        String propertyModel = normalize(properties.getOpenrouter().getModel());
        String envModel = normalize(System.getenv("OPENROUTER_MODEL"));
        boolean propertyModelPresent = !propertyModel.isBlank();
        boolean envModelPresent = !envModel.isBlank();
        this.model = propertyModelPresent ? propertyModel : (envModelPresent ? envModel : "openai/gpt-4.1-mini");

        String baseUrl = normalize(properties.getOpenrouter().getBaseUrl());
        if (baseUrl.isBlank()) {
            baseUrl = "https://openrouter.ai/api/v1";
        }

        log.info("[AI-DEBUG] afetch.openrouter.api-key present: {}", propertyApiKeyPresent);
        log.info("[AI-DEBUG] OPENROUTER_API_KEY env present: {}", envApiKeyPresent);
        log.info("[AI-DEBUG] resolved OPENROUTER_API_KEY present: {}", !this.apiKey.isBlank());
        log.info("[AI-DEBUG] afetch.openrouter.model present: {}", propertyModelPresent);
        log.info("[AI-DEBUG] OPENROUTER_MODEL env present: {}", envModelPresent);
        log.info("[AI-DEBUG] resolved OPENROUTER_MODEL: {}", this.model);
        log.info("[AI-DEBUG] OpenRouter base URL: {}", baseUrl);

        WebClient.Builder builder = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("HTTP-Referer", "https://afetch.local");

        if (!apiKey.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + apiKey);
            log.info("[AI] Initializing OpenRouter provider");
        } else {
            log.warn("[AI] OpenRouter provider disabled because API key is not configured");
        }

        this.webClient = builder.build();
    }

    @SuppressWarnings("unchecked")
    public String chat(List<Map<String, String>> messages) {
        if (apiKey.isBlank()) {
            log.warn("[AI-DEBUG] OpenRouter chat called without API key configured");
            throw new IllegalStateException(
                    "OpenRouter API key is not configured. Set the OPENROUTER_API_KEY environment variable or `afetch.openrouter.api-key` in application.yml to enable full assistant answers."
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

            log.info("[AI] OpenRouter provider ready");
            return content.toString();
        } catch (WebClientResponseException e) {
            String errorMessage = String.format("OpenRouter API error: %d - %s", e.getStatusCode().value(), e.getResponseBodyAsString());
            log.error("[AI] OpenRouter API request failed: {}", errorMessage);
            throw new IllegalStateException(errorMessage, e);
        } catch (IllegalStateException e) {
            log.warn("[AI] OpenRouter provider unavailable: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[AI] OpenRouter communication failure", e);
            throw new IllegalStateException("Failed to communicate with OpenRouter API: " + e.getMessage(), e);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
