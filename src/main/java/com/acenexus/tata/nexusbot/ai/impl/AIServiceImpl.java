package com.acenexus.tata.nexusbot.ai.impl;

import com.acenexus.tata.nexusbot.ai.AIService;
import com.acenexus.tata.nexusbot.config.properties.GeminiProxyProperties;
import com.acenexus.tata.nexusbot.config.properties.GroqProperties;
import com.acenexus.tata.nexusbot.constants.AiModel;
import com.acenexus.tata.nexusbot.constants.AiProvider;
import com.acenexus.tata.nexusbot.entity.ChatMessage;
import com.acenexus.tata.nexusbot.repository.ChatMessageRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {
    private static final Logger logger = LoggerFactory.getLogger(AIServiceImpl.class);

    private final GroqProperties groqProperties;
    private final GeminiProxyProperties geminiProxyProperties;
    private final ChatMessageRepository chatMessageRepository;

    @Value("${ai.conversation.history-limit:15}")
    private int historyLimit;

    private final Map<AiProvider, WebClient> clientMap = new EnumMap<>(AiProvider.class);

    @PostConstruct
    public void init() {
        clientMap.put(AiProvider.GROQ, WebClient.builder()
                .baseUrl(groqProperties.getUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + groqProperties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build());
        logger.info("AIService registered provider: GROQ, url: {}", groqProperties.getUrl());

        clientMap.put(AiProvider.GEMINI_PROXY, WebClient.builder()
                .baseUrl(geminiProxyProperties.getUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + geminiProxyProperties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build());
        logger.info("AIService registered provider: GEMINI_PROXY, url: {}", geminiProxyProperties.getUrl());
    }

    @Override
    public ChatResponse chatWithContext(String roomId, String message, String selectedModel) {
        if (message == null || message.trim().isEmpty()) {
            return new ChatResponse(null, selectedModel, 0, 0L, false);
        }

        AiModel aiModel = AiModel.fromId(selectedModel);
        AiProvider targetProvider = aiModel.provider;
        WebClient client = clientMap.get(targetProvider);

        if (client == null) {
            logger.warn("No WebClient for provider: {}, model: {}", targetProvider, selectedModel);
            return new ChatResponse(null, selectedModel, 0, 0L, false);
        }

        long startTime = System.currentTimeMillis();

        try {
            // 獲取最近的對話歷史
            List<ChatMessage> recentHistory = chatMessageRepository.findRecentMessagesDesc(roomId, historyLimit);

            // 建立包含歷史對話的訊息列表
            List<Map<String, String>> messages = buildMessagesWithHistory(recentHistory, message);

            var request = Map.of(
                    "model", selectedModel,
                    "messages", messages,
                    "temperature", aiModel.temperature,
                    "max_tokens", aiModel.maxTokens
            );

            var response = client
                    .post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(15))
                    .subscribeOn(Schedulers.boundedElastic())
                    .block();

            long processingTime = System.currentTimeMillis() - startTime;
            logger.debug("AI response - provider: {}, model: {}", targetProvider, selectedModel);

            return parseAiResponse(response, processingTime, selectedModel);

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            logger.error("AI call failed - Provider: {}, Model: {}, Time: {}ms, Error: {}", targetProvider, selectedModel, processingTime, e.getMessage(), e);
            return new ChatResponse(null, selectedModel, 0, processingTime, false);
        }
    }

    /**
     * 建立包含歷史對話的訊息列表
     */
    private List<Map<String, String>> buildMessagesWithHistory(List<ChatMessage> history, String currentMessage) {
        List<Map<String, String>> messages = new ArrayList<>();

        messages.add(Map.of("role", "system", "content", """
                你是一個「知識型好朋友」的聊天夥伴，主要任務是用輕鬆自然的方式回答使用者的問題。

                【角色定位】
                - 就像一個懂很多的朋友，不是嚴肅的專家。
                - 回答自然直接，不用太正式。
                - 給的建議要實際、好懂，不要太複雜。

                【回應原則】
                1. 回答用繁體中文。
                2. 回應簡潔有重點，不超過 200 字。
                3. 口語化，像朋友聊天，但避免使用「咧」「～」這類語氣符號。
                4. 不知道就直說「不知道」，不要亂編。
                5. 少用表情符號，重點放在內容。
                6. 保持對話連貫，記得前後文。
                7. 適度給些小提醒或小建議，但不要太囉嗦。

                【範例】
                使用者：你覺得 AI 會取代人類嗎？
                AI：不會，AI 比較像工具，處理重複或麻煩的工作，人類還是需要的。

                使用者：明天台中天氣怎樣？
                AI：這我不確定，你可以查氣象局的預報，會比較準。
                """));

        // 加入歷史對話
        for (ChatMessage historyMsg : history) {
            String role = (historyMsg.getMessageType() == ChatMessage.MessageType.USER) ? "user" : "assistant";
            messages.add(Map.of("role", role, "content", historyMsg.getContent()));
        }

        // 加入當前使用者訊息
        messages.add(Map.of("role", "user", "content", currentMessage));

        logger.debug("Built message context with {} history messages", history.size());
        return messages;
    }

    /**
     * 解析 AI API 回應（OpenAI Chat Completions 通用格式）
     */
    private ChatResponse parseAiResponse(Map<?, ?> response, long processingTime, String usedModel) {
        if (response == null) {
            return new ChatResponse(null, usedModel, 0, processingTime, false);
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
            int tokensUsed = 0;
            var usage = (Map<?, ?>) response.get("usage");
            if (usage != null) {
                var totalTokens = usage.get("total_tokens");
                if (totalTokens instanceof Number) {
                    tokensUsed = ((Number) totalTokens).intValue();
                }
                logger.debug("Tokens used: {}", tokensUsed);
            }

            return new ChatResponse(content, usedModel, tokensUsed, processingTime, content != null);

        } catch (Exception e) {
            logger.error("Failed to parse AI response: {}", e.getMessage());
            return new ChatResponse(null, usedModel, 0, processingTime, false);
        }
    }
}