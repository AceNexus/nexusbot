package com.acenexus.tata.nexusbot.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
public class GroqService {

    @Value("${groq.api-key:}")
    private String apiKey;

    @Value("${groq.model:llama-3.1-8b-instant}")
    private String model;

    private volatile WebClient webClient;
    private volatile boolean isConfigured = false;

    @PostConstruct
    public void init() {
        if (!apiKey.isEmpty()) {
            webClient = WebClient.builder()
                    .baseUrl("https://api.groq.com/openai/v1")
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();
            isConfigured = true;
            log.info("GroqService initialized with model: {}", model);
        } else {
            log.warn("Groq API key not configured, AI responses disabled");
        }
    }

    public String chat(String message) {
        if (!isConfigured) {
            return null;
        }

        if (message == null || message.trim().isEmpty()) {
            return null;
        }

        try {
            var request = Map.of(
                    "model", model,
                    "messages", new Object[]{
                            Map.of("role", "system", "content", "你是友善的聊天機器人，用繁體中文簡潔回應。"),
                            Map.of("role", "user", "content", message)
                    },
                    "temperature", 0.7,
                    "max_tokens", 1000
            );

            var response = webClient
                    .post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();

            return extractContent(response);

        } catch (Exception e) {
            log.error("Groq API call failed: {}", e.getMessage());
            return null;
        }
    }

    private String extractContent(Map<?, ?> response) {
        if (response == null) {
            return null;
        }

        try {
            var choices = (java.util.List<?>) response.get("choices");
            if (choices != null && !choices.isEmpty()) {
                var choice = (Map<?, ?>) choices.get(0);
                var messageObj = (Map<?, ?>) choice.get("message");
                var content = (String) messageObj.get("content");
                return (content != null) ? content.trim() : null;
            }
        } catch (Exception e) {
            log.error("Failed to parse Groq response: {}", e.getMessage());
        }

        return null;
    }
}