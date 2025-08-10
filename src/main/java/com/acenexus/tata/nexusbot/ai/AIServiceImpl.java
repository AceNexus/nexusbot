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

    public ChatResponse chatWithDetails(String message) {
        if (!isConfigured) {
            return new ChatResponse(null, model, 0, 0L, false);
        }

        if (message == null || message.trim().isEmpty()) {
            return new ChatResponse(null, model, 0, 0L, false);
        }

        long startTime = System.currentTimeMillis();

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

            long processingTime = System.currentTimeMillis() - startTime;
            logger.debug("Groq API response: {}", response);

            return parseGroqResponse(response, processingTime);

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            logger.error("Groq API call failed: {}", e.getMessage());
            return new ChatResponse(null, model, 0, processingTime, false);
        }
    }

    /**
     * 解析 Groq API 回應，一次性提取所有需要的資料
     *
     * @param response       Groq API 原始回應
     * @param processingTime 處理時間
     * @return ChatResponse 物件
     */
    private ChatResponse parseGroqResponse(Map<?, ?> response, long processingTime) {
        if (response == null) {
            return new ChatResponse(null, model, 0, processingTime, false);
        }

        try {
            // 解析 content
            String content = null;
            var choices = (java.util.List<?>) response.get("choices");
            if (choices != null && !choices.isEmpty()) {
                var choice = (Map<?, ?>) choices.get(0);
                var messageObj = (Map<?, ?>) choice.get("message");
                if (messageObj != null) {
                    var rawContent = (String) messageObj.get("content");
                    content = (rawContent != null) ? rawContent.trim() : null;
                }
            }

            // 解析 tokens
            Integer tokensUsed = 0;
            var usage = (Map<?, ?>) response.get("usage");
            if (usage != null) {
                var totalTokens = usage.get("total_tokens");
                if (totalTokens instanceof Number) {
                    tokensUsed = ((Number) totalTokens).intValue();
                }
                logger.debug("Tokens used: {}", tokensUsed);
            }

            return new ChatResponse(content, model, tokensUsed, processingTime, content != null);

        } catch (Exception e) {
            logger.error("Failed to parse Groq response: {}", e.getMessage());
            return new ChatResponse(null, model, 0, processingTime, false);
        }
    }
}