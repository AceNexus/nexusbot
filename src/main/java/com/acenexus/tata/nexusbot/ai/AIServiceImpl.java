package com.acenexus.tata.nexusbot.ai;

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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {
    private static final Logger logger = LoggerFactory.getLogger(AIServiceImpl.class);

    @Value("${groq.api-key:}")
    private String apiKey;

    @Value("${groq.model:llama-3.1-8b-instant}")
    private String model;

    @Value("${ai.conversation.history-limit:15}")
    private int historyLimit;

    private volatile WebClient webClient;
    private volatile boolean isConfigured = false;

    private final ChatMessageRepository chatMessageRepository;

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

    @Override
    public ChatResponse chatWithContext(String roomId, String message, String selectedModel) {
        if (!isConfigured) {
            return new ChatResponse(null, selectedModel, 0, 0L, false);
        }

        if (message == null || message.trim().isEmpty()) {
            return new ChatResponse(null, selectedModel, 0, 0L, false);
        }

        long startTime = System.currentTimeMillis();

        try {
            // 獲取最近的對話歷史
            List<ChatMessage> recentHistory = chatMessageRepository.findRecentMessagesDesc(roomId, historyLimit);

            // 建立包含歷史對話的訊息列表
            List<Map<String, String>> messages = buildMessagesWithHistory(recentHistory, message);

            // 根據不同模型設定最適合的參數
            var modelConfig = getModelConfiguration(selectedModel);
            
            var request = Map.of(
                    "model", selectedModel,
                    "messages", messages,
                    "temperature", modelConfig.temperature(),
                    "max_tokens", modelConfig.maxTokens()
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
            logger.debug("Groq API response with context using model {}: {}", selectedModel, response);

            return parseGroqResponse(response, processingTime, selectedModel);

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            logger.error("Groq API call with context failed using model {}: {}", selectedModel, e.getMessage());
            return new ChatResponse(null, selectedModel, 0, processingTime, false);
        }
    }

    /**
     * 模型配置記錄
     */
    private record ModelConfiguration(double temperature, int maxTokens) {}

    /**
     * 根據模型獲取最佳配置參數
     */
    private ModelConfiguration getModelConfiguration(String model) {
        return switch (model) {
            // 快速模型 - 較高創意性，適中長度
            case "llama-3.1-8b-instant" -> new ModelConfiguration(0.8, 800);
            
            // 大型強力模型 - 較低溫度確保準確性，更長輸出
            case "llama-3.3-70b-versatile" -> new ModelConfiguration(0.6, 1200);
            case "llama3-70b-8192" -> new ModelConfiguration(0.65, 1500);
            
            // 創意模型 - 高創意性，中等長度
            case "gemma2-9b-it" -> new ModelConfiguration(0.9, 1000);
            
            // 推理模型 - 低溫度確保邏輯性，較長輸出
            case "deepseek-r1-distill-llama-70b" -> new ModelConfiguration(0.4, 1500);
            
            // 多語言模型 - 平衡設定
            case "qwen/qwen3-32b" -> new ModelConfiguration(0.7, 1200);
            
            // 預設設定
            default -> new ModelConfiguration(0.7, 1000);
        };
    }

    /**
     * 建立包含歷史對話的訊息列表
     */
    private List<Map<String, String>> buildMessagesWithHistory(List<ChatMessage> history, String currentMessage) {
        List<Map<String, String>> messages = new ArrayList<>();

        messages.add(Map.of("role", "system", "content", """
                你是個知識豐富的朋友，回答問題時自然直接。
                                
                回應原則：
                1. 用繁體中文回答
                2. 簡潔明瞭，不超過 200 字
                3. 口語化，就像跟朋友聊天
                4. 不知道就說不知道，別硬掰
                5. 避免太正式或太假掰的語氣
                6. 少用表情符號，專注回答內容
                7. 記住之前的對話內容，保持對話的連貫性
                                
                就像平常聊天一樣輕鬆回應就好。
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
     * 解析 Groq API 回應，一次性提取所有需要的資料
     *
     * @param response       Groq API 原始回應
     * @param processingTime 處理時間
     * @param usedModel      使用的模型
     * @return ChatResponse 物件
     */
    private ChatResponse parseGroqResponse(Map<?, ?> response, long processingTime, String usedModel) {
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
            Integer tokensUsed = 0;
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
            logger.error("Failed to parse Groq response: {}", e.getMessage());
            return new ChatResponse(null, usedModel, 0, processingTime, false);
        }
    }
}