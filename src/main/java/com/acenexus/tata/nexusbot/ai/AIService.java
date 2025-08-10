package com.acenexus.tata.nexusbot.ai;

/**
 * AI 聊天服務介面
 */
public interface AIService {

    /**
     * 處理聊天對話
     *
     * @param message 使用者訊息
     * @return AI 回應結果（包含模型、tokens等資訊）
     */
    ChatResponse chatWithDetails(String message);

    /**
     * AI 回應結果
     */
    record ChatResponse(
            String content,
            String model,
            Integer tokensUsed,
            Long processingTime,
            boolean success
    ) {
    }
}