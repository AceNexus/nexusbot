package com.acenexus.tata.nexusbot.ai;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Service
public class AIServiceImpl implements AIService {
    private static final Logger logger = LoggerFactory.getLogger(AIServiceImpl.class);

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
            logger.info("GroqService initialized with model: {}", model);
        } else {
            logger.warn("Groq API key not configured, AI responses disabled");
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
                            Map.of("role", "system", "content", """
                                    你是個知識豐富的朋友，回答問題時自然直接。
                                    
                                    回應原則：
                                    1. 用繁體中文回答
                                    2. 簡潔明瞭，不超過 200 字
                                    3. 口語化，就像跟朋友聊天
                                    4. 不知道就說不知道，別硬掰
                                    5. 避免太正式或太假掰的語氣
                                    6. 少用表情符號，專注回答內容
                                    
                                    就像平常聊天一樣輕鬆回應就好。
                                    """),
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
            logger.error("Groq API call failed: {}", e.getMessage());
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
            logger.error("Failed to parse Groq response: {}", e.getMessage());
        }

        return null;
    }
}